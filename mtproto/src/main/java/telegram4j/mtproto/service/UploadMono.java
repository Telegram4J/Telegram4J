package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.MTProtoClientGroupManager;
import telegram4j.mtproto.util.CryptoUtil;
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

    // It's necessary to avoid connection reset by server
    static final Duration UPLOAD_INTERVAL = Duration.ofMillis(300);

    private final MTProtoClientGroupManager groupManager;
    private final UploadOptions options;
    private final long fileId = CryptoUtil.random.nextLong();

    UploadMono(MTProtoClientGroupManager groupManager, UploadOptions options) {
        this.groupManager = groupManager;
        this.options = options;
    }

    @Override
    public void subscribe(CoreSubscriber<? super InputFile> actual) {
        options.getData().subscribe(new UploadSubscriber(actual));
    }

    class UploadSubscriber extends BaseSubscriber<ByteBuf> {
        private final CoreSubscriber<? super InputFile> actual;

        private int readParts;
        private CompositeByteBuf buffer;
        private MessageDigest md5;
        private int roundRobin;

        private final AtomicInteger received = new AtomicInteger();

        public UploadSubscriber(CoreSubscriber<? super InputFile> actual) {
            this.actual = actual;
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            if (options.isBigFile()) {
                md5 = null;
            } else {
                try {
                    md5 = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    onError(e);
                }
            }

            AtomicInteger pending = new AtomicInteger(options.getParallelism());
            for (int i = 1; i <= options.getParallelism(); i++) {
                DcId dcId = groupManager.mainId().shift(i);
                var child = groupManager.getOrCreateMediaClient(dcId, groupManager.main().getDatacenter());

                child.state()
                        .filter(s -> s == MTProtoClient.State.READY)
                        .next()
                        .doOnNext(s -> {
                            if (pending.decrementAndGet() == 0) {
                                log.debug("All upload clients connected");
                                subscription.request(options.getPartsCount());
                                actual.onSubscribe(this);
                            }
                        })
                        .subscribe();

                child.connect().subscribe();
            }
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            actual.onError(throwable);
        }

        @Override
        protected void hookOnNext(ByteBuf buf) {
            // TODO: synchronization?

            // aligned buffer
            if (buf.readableBytes() % options.getPartSize() == 0) {
                while (buf.isReadable()) {
                    send(buf.readRetainedSlice(options.getPartSize()));
                    readParts++;
                }
            }

            if (buffer == null)
                buffer = UnpooledByteBufAllocator.DEFAULT.compositeHeapBuffer();

            buffer.addFlattenedComponents(true, buf);
            while (buffer.isReadable(options.getPartSize())) {
                send(buffer.readRetainedSlice(options.getPartSize()));
                readParts++;
            }

            // latest part or part size is too large and file can be sent as one part
            if (readParts == options.getPartsCount() - 1) {
                if (buffer.isReadable()) // the last part can be misaligned
                    send(buffer);
                onComplete();
            }
        }

        void send(ByteBuf buf) {
            if (md5 != null)
                md5.update(buf.nioBuffer());

            SaveFilePart part;
            try {
                part = ImmutableSaveFilePart.of(fileId, readParts, buf);
            } finally {
                ReferenceCountUtil.safeRelease(buf);
            }

            int idx = roundRobin++;
            if (roundRobin == options.getParallelism()) {
                roundRobin = 0;
            }

            // idx is one-based to prevent using main client as uploader
            DcId dcId = groupManager.mainId().shift(idx + 1);

            if (log.isDebugEnabled()) {
                log.debug("[DC:{}, F:{}] Preparing to send {}/{}", dcId, fileId, part.filePart() + 1, options.getPartsCount());
            }

            Mono.delay(UPLOAD_INTERVAL)
                    .then(groupManager.send(dcId, part))
                    .subscribe(res -> {
                        if (!res) throw new IllegalStateException("Unexpected result state");

                        int cnt = received.incrementAndGet();
                        if (log.isDebugEnabled()) {
                            log.debug("[DC:{}, F:{}] Uploaded part {}, {}/{}", dcId, fileId,
                                    part.filePart() + 1, cnt, options.getPartsCount());
                        }

                        if (cnt == options.getPartsCount()) {
                            if (options.isBigFile()) {
                                actual.onNext(ImmutableInputFileBig.of(fileId, options.getPartsCount(), options.getName()));
                            } else {
                                actual.onNext(ImmutableBaseInputFile.of(fileId, options.getPartsCount(),
                                        options.getName(), ByteBufUtil.hexDump(md5.digest())));
                            }
                            actual.onComplete();
                        }
                    }, this::onError);
        }
    }
}
