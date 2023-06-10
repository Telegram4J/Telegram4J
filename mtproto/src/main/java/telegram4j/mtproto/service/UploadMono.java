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
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.request.upload.ImmutableSaveBigFilePart;
import telegram4j.tl.request.upload.ImmutableSaveFilePart;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

        final AtomicInteger readParts = new AtomicInteger();
        CompositeByteBuf buffer;
        MessageDigest md5;
        final AtomicInteger roundRobin = new AtomicInteger();

        final CoreSubscriber<? super InputFile> actual;
        final MTProtoClientGroup clientGroup;
        final UploadOptions options;
        final AtomicInteger received = new AtomicInteger();
        final AtomicInteger requested = new AtomicInteger();

        final AtomicReference<Subscription> subscription = new AtomicReference<>();

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

            int partId = readParts.getAndIncrement();
            TlMethod<Boolean> part;
            try {
                if (options.isBigFile()) {
                    part = ImmutableSaveBigFilePart.of(options.getFileId(), partId, options.getPartsCount(), buf);
                } else {
                    part = ImmutableSaveFilePart.of(options.getFileId(), partId, buf);
                }
            } finally {
                ReferenceCountUtil.safeRelease(buf);
            }

            int idx = Math.abs(roundRobin.getAndIncrement() % options.getParallelism());

            DcId dcId = DcId.upload(clientGroup.main().dc().getId(), idx);
            if (log.isDebugEnabled()) {
                log.debug("[DC:{}, F:{}] Preparing to send {}/{}", dcId, options.getFileId(),
                        partId + 1, options.getPartsCount());
            }

            Mono.delay(UPLOAD_INTERVAL, Schedulers.single())
                    .then(clientGroup.send(dcId, part))
                    .subscribe(res -> {
                        if (!res) throw new IllegalStateException("Unexpected result state");

                        int cnt = received.incrementAndGet();
                        if (log.isDebugEnabled()) {
                            log.debug("[DC:{}, F:{}] Uploaded part {}, {}/{}", dcId, options.getFileId(),
                                    partId + 1, cnt, options.getPartsCount());
                        }

                        int d = requested.decrementAndGet();
                        if (cnt == options.getPartsCount()) {
                            completeInner();
                        } else if (d == 0) {
                            int remain = options.getPartsCount() - readParts.get();
                            int r = Math.min(options.getParallelism(), remain);
                            if (requested.compareAndSet(0, r)) {
                                subscription.get().request(r);
                            }
                        }
                    }, t -> {
                        if (log.isDebugEnabled()) {
                            log.debug("[DC:{}, F:{}] Failed to upload file part: {}",
                                    dcId, options.getFileId(), partId);
                        }
                        onError(t);
                    });
        }

        void completeInner() {
            var sub = subscription.getAndSet(Operators.cancelledSubscription());
            if (sub == Operators.cancelledSubscription()) {
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
            if (subscription.compareAndSet(null, s)) {
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(ByteBuf buf) {
            var current = subscription.get();
            if (current == Operators.cancelledSubscription()) {
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

            if (readParts.get() == options.getPartsCount() - 1 && buffer.isReadable()) {
                send(buffer);
            }
        }

        @Override
        public void onError(Throwable t) {
            var current = subscription.getAndSet(Operators.cancelledSubscription());
            if (current == Operators.cancelledSubscription()) {
                Operators.onErrorDropped(t, currentContext());
                return;
            }

            actual.onError(t);
        }

        @Override
        public void onComplete() {
            subscription.set(Operators.cancelledSubscription());
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
                    DcId dcId = DcId.upload(clientGroup.main().dc().getId(), i);

                    clientGroup.getOrCreateClient(dcId)
                            .subscribe(client -> {
                                if (pending.decrementAndGet() == 0) {
                                    subscription.get().request(Math.min(options.getParallelism(), options.getPartsCount()));
                                }
                            }, this::onError);
                }
            }
        }

        @Override
        public void cancel() {
            var current = subscription.getAndSet(Operators.cancelledSubscription());
            if (current == Operators.cancelledSubscription()) {
                return;
            }

            current.cancel();
        }

        @Nullable
        @Override
        public Object scanUnsafe(Scannable.Attr key) {
            if (key == Attr.TERMINATED) return subscription.get() == Operators.cancelledSubscription();
            if (key == Attr.PARENT) return subscription.get();
            if (key == Attr.RUN_STYLE) return Attr.RunStyle.ASYNC;
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
