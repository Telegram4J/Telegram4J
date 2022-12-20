package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;
import reactor.util.context.Context;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.tl.ImmutableBaseInputFile;
import telegram4j.tl.ImmutableInputFileBig;
import telegram4j.tl.InputFile;
import telegram4j.tl.request.upload.ImmutableSaveFilePart;
import telegram4j.tl.request.upload.SaveFilePart;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static telegram4j.mtproto.service.UploadService.log;

class UploadMono extends Mono<InputFile> {

    private final MTProtoClientGroup clientGroup;
    private final UploadOptions options;

    UploadMono(MTProtoClientGroup clientGroup, UploadOptions options) {
        this.clientGroup = clientGroup;
        this.options = options;
    }

    @Override
    public void subscribe(CoreSubscriber<? super InputFile> actual) {
        options.getData().subscribe(new UploadSubscriber(actual, clientGroup, options));
    }

    static class UploadSubscriber implements CoreSubscriber<ByteBuf>, Scannable, Subscription {
        // It's necessary to avoid connection reset by server
        static final Duration UPLOAD_INTERVAL = Duration.ofMillis(300);

        static final int CANCELLED = 1 << 0;
        static final int TERMINATED = 1 << 1;

        int readParts;
        CompositeByteBuf buffer;
        MessageDigest md5;
        int roundRobin;

        final CoreSubscriber<? super InputFile> actual;
        final MTProtoClientGroup clientGroup;
        final UploadOptions options;
        final AtomicInteger received = new AtomicInteger();
        final AtomicInteger requested = new AtomicInteger();

        Subscription subscription;
        int state;

        public UploadSubscriber(CoreSubscriber<? super InputFile> actual,
                                MTProtoClientGroup clientGroup,
                                UploadOptions options) {
            this.actual = actual;
            this.clientGroup = clientGroup;
            this.options = options;

            this.requested.setRelease(options.getParallelism());
        }

        void send(ByteBuf buf) {
            if (md5 != null)
                md5.update(buf.nioBuffer());

            SaveFilePart part;
            try {
                part = ImmutableSaveFilePart.of(options.getFileId(), readParts++, buf);
            } finally {
                ReferenceCountUtil.safeRelease(buf);
            }

            int idx = roundRobin++;
            if (roundRobin == options.getParallelism()) {
                roundRobin = 0;
            }

            DcId dcId = DcId.upload(clientGroup.main().getDatacenter().getId(), idx);
            if (log.isDebugEnabled()) {
                log.debug("[DC:{}, F:{}] Preparing to send {}/{}", dcId, options.getFileId(),
                        part.filePart() + 1, options.getPartsCount());
            }

            Mono.delay(UPLOAD_INTERVAL)
                    .then(clientGroup.send(dcId, part))
                    .subscribe(res -> {
                        if (!res) throw new IllegalStateException("Unexpected result state");

                        int cnt = received.incrementAndGet();
                        if (log.isDebugEnabled()) {
                            log.debug("[DC:{}, F:{}] Uploaded part {}, {}/{}", dcId, options.getFileId(),
                                    part.filePart() + 1, cnt, options.getPartsCount());
                        }

                        int d = requested.decrementAndGet();
                        if (cnt == options.getPartsCount()) {
                            completeInner();
                        } else if (d == 0) {
                            int remain = options.getPartsCount() - readParts;
                            int r = Math.min(options.getParallelism(), remain);
                            requested.set(r);
                            subscription.request(r);
                        }
                    }, this::onError);
        }

        void completeInner() {
            if ((state & CANCELLED) != 0) {
                return;
            }

            if (options.isBigFile()) {
                actual.onNext(ImmutableInputFileBig.of(options.getFileId(), options.getPartsCount(), options.getName()));
            } else {
                actual.onNext(ImmutableBaseInputFile.of(options.getFileId(), options.getPartsCount(),
                        options.getName(), ByteBufUtil.hexDump(md5.digest())));
            }
            actual.onComplete();
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (Operators.validate(this.subscription, s)) {
                this.subscription = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(ByteBuf buf) {
            if ((state & TERMINATED) != 0) {
                Operators.onNextDropped(buf, currentContext());
                return;
            }

            // aligned buffer
            if (buf.readableBytes() % options.getPartSize() == 0) {
                while (buf.isReadable()) {
                    send(buf.readRetainedSlice(options.getPartSize()));
                }
                ReferenceCountUtil.release(buf);
                return;
            }

            if (buffer == null) {
                buffer = UnpooledByteBufAllocator.DEFAULT.compositeHeapBuffer();
            }

            buffer.addFlattenedComponents(true, buf);
            while (buffer.isReadable(options.getPartSize())) {
                send(buffer.readRetainedSlice(options.getPartSize()));
            }

            if (readParts == options.getPartsCount() - 1 && buffer.isReadable()) {
                send(buffer);
            }
        }

        @Override
        public void onError(Throwable t) {
            if ((state & TERMINATED) != 0) {
                Operators.onErrorDropped(t, currentContext());
                return;
            }

            state |= TERMINATED;
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if ((state & TERMINATED) != 0) {
                return;
            }
            state |= TERMINATED;
        }

        @Override
        public void request(long n) {
            if (Operators.validate(n)) {
                if (options.isBigFile()) {
                    md5 = null;
                } else {
                    try {
                        md5 = MessageDigest.getInstance("MD5");
                    } catch (NoSuchAlgorithmException e) {
                        onError(e);
                        return;
                    }
                }

                AtomicInteger pending = new AtomicInteger(options.getParallelism());
                for (int i = 0; i < options.getParallelism(); i++) {
                    DcId dcId = DcId.upload(clientGroup.main().getDatacenter().getId(), i);

                    clientGroup.getOrCreateClient(dcId)
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(client -> {
                                if (pending.decrementAndGet() == 0) {
                                    subscription.request(Math.min(options.getParallelism(), options.getPartsCount()));
                                }
                            }, this::onError);
                }
            }
        }

        @Override
        public void cancel() {
            if ((state & CANCELLED) != 0) {
                return;
            }
            state |= CANCELLED;
            subscription.cancel();
        }

        @Nullable
        @Override
        public Object scanUnsafe(Scannable.Attr key) {
            if (key == Attr.TERMINATED) return (state & TERMINATED) != 0;
            if (key == Attr.PARENT) return subscription;
            if (key == Attr.RUN_STYLE) return Attr.RunStyle.ASYNC;
            if (key == Attr.CANCELLED) return (state & CANCELLED) != 0;
            if (key == Attr.REQUESTED_FROM_DOWNSTREAM) return options.getPartsCount();
            if (key == Attr.ACTUAL) return actual;
            return null;
        }

        @Override
        public Context currentContext() {
            return actual.currentContext();
        }
    }
}
