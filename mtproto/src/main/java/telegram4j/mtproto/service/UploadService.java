package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.ReferenceCountUtil;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.netty.ByteBufFlux;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;
import telegram4j.tl.request.upload.*;
import telegram4j.tl.storage.FileType;
import telegram4j.tl.upload.BaseFile;
import telegram4j.tl.upload.WebFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static telegram4j.mtproto.util.EmissionHandlers.DEFAULT_PARKING;

public class UploadService extends RpcService {
    public static final int MIN_PART_SIZE = 1024;
    public static final int MAX_PART_SIZE = 512 * 1024;
    // it is necessary that the server does not consider our frequent packets to be a flood
    private static final Duration UPLOAD_INTERVAL = Duration.ofMillis(300);
    private static final int TEN_MB = 10 * 1024 * 1024;
    private static final int PARALLELISM = 3;
    private static final int PRECISE_LIMIT = 1024 * 1024;

    public UploadService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public static int suggestPartSize(int size, int partSize) {
        if (partSize == -1) {
            return MAX_PART_SIZE; // TODO
        } else if (partSize > MAX_PART_SIZE) {
            throw new IllegalArgumentException(size + " > " + MAX_PART_SIZE);
        } else if (partSize < MIN_PART_SIZE) {
            throw new IllegalArgumentException(size + " < " + MIN_PART_SIZE);
        } else {
            return partSize;
        }
    }

    // additional methods
    // =========================
    // TODO list:
    // upload.getCdnFile#395f69da file_token:bytes offset:long limit:int = upload.CdnFile;
    // upload.reuploadCdnFile#9b2754a8 file_token:bytes request_token:bytes = Vector<FileHash>;
    // upload.getCdnFileHashes#91dc3f31 file_token:bytes offset:long = Vector<FileHash>;

    @BotCompatible
    public Mono<InputFile> saveFile(ByteBufFlux data, int size, int partSize, String name) {
        return Mono.defer(() -> {
            // TODO: perhaps necessary checks:
            //  - check size
            //  - verify final counter.value with computed parts count
            //  - check part size
            long fileId = CryptoUtil.random.nextLong();

            IntHolder counter = new IntHolder();
            CompositeByteBuf agr = ByteBufAllocator.DEFAULT.compositeBuffer();
            if (size > TEN_MB) {
                int parts = (int) Math.ceil((float) size / partSize);

                Sinks.Many<SaveBigFilePart> queue = Sinks.many().multicast()
                        .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

                AtomicInteger it = new AtomicInteger();
                AtomicInteger suc = new AtomicInteger();

                List<MTProtoClient> clients = new ArrayList<>(PARALLELISM);
                DataCenter mediaDc = DataCenter.mediaDataCentersIpv4.stream()
                        .filter(dc -> dc.getId() == client.getDatacenter().getId())
                        .findFirst()
                        .orElseThrow();

                Mono<Void> initialize = Flux.range(0, PARALLELISM)
                        .map(i -> client.createMediaClient(mediaDc))
                        .doOnNext(clients::add)
                        .flatMap(MTProtoClient::connect)
                        .then();

                Mono<Void> sender = queue.asFlux()
                        .publishOn(Schedulers.boundedElastic())
                        .handle((req, sink) -> {
                            MTProtoClient client = clients.get(it.getAndUpdate(i -> i + 1 == PARALLELISM ? 0 : i + 1));

                            client.sendAwait(req)
                                    .flatMap(b -> {
                                        if (suc.incrementAndGet() == req.fileTotalParts()) {
                                            queue.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                                            return Flux.fromIterable(clients)
                                                    .flatMap(MTProtoClient::close)
                                                    .then(Mono.fromRunnable(sink::complete));
                                        }
                                        return Mono.empty();
                                    })
                                    .subscribe();
                        })
                        .then();

                Mono<Void> reader = data.flatMap(bufferUntil(agr, partSize))
                        .map(part -> {
                            try {
                                return ImmutableSaveBigFilePart.of(fileId, counter.value++, parts, part);
                            } finally {
                                ReferenceCountUtil.safeRelease(part);
                            }
                        })
                        .doOnNext(buf -> queue.emitNext(buf, DEFAULT_PARKING))
                        .then();

                return Mono.when(initialize, reader, sender)
                        .then(Mono.fromSupplier(() -> ImmutableInputFileBig.of(fileId, parts, name)));
            }

            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw Exceptions.propagate(e);
            }

            return data.flatMap(bufferUntil(agr, partSize))
            .flatMap(part -> Mono.fromCallable(() -> {
                md5.update(part.nioBuffer());
                try {
                    return ImmutableSaveFilePart.of(fileId, counter.value++, part);
                } finally {
                    ReferenceCountUtil.safeRelease(part);
                }
            })
            .delayElement(UPLOAD_INTERVAL))
            .flatMap(client::sendAwait)
            .then(Mono.fromSupplier(() -> ImmutableBaseInputFile.of(fileId,
                    counter.value, name, ByteBufUtil.hexDump(md5.digest()))));
        });
    }

    static Function<ByteBuf, Flux<ByteBuf>> bufferUntil(CompositeByteBuf agr, int partSize) {
        return buf -> Flux.create(sink -> {
            if (buf.readableBytes() == partSize) {
                sink.next(buf);
                return;
            }

            agr.addFlattenedComponents(true, buf);
            while (agr.isReadable(partSize)) {
                sink.next(agr.readSlice(partSize));
            }

            if (agr.isReadable()) { // the last part
                sink.next(agr);
                sink.complete();
            }
        });
    }

    static class IntHolder {
        private int value;
    }

    // upload namespace
    // =========================

    @BotCompatible
    public Mono<Boolean> saveFilePart(long fileId, int filePart, ByteBuf bytes) {
        return Mono.defer(() -> client.sendAwait(ImmutableSaveFilePart.of(fileId, filePart, bytes)));
    }

    @BotCompatible
    public Flux<BaseFile> getFile(InputFileLocation location) {
        return Flux.defer(() -> {
            AtomicInteger offset = new AtomicInteger();
            AtomicBoolean complete = new AtomicBoolean();
            int limit = PRECISE_LIMIT;
            ImmutableGetFile request = ImmutableGetFile.of(ImmutableGetFile.PRECISE_MASK, location, 0, limit);

            return Flux.defer(() -> client.sendAwait(request.withOffset(offset.get())))
                    .cast(BaseFile.class)
                    .mapNotNull(part -> {
                        offset.addAndGet(limit);
                        if (part.type() == FileType.UNKNOWN || !part.bytes().isReadable()) { // download completed
                            complete.set(true);
                            return null;
                        }

                        return part;
                    })
                    .repeat(() -> !complete.get());
        });
    }

    @BotCompatible
    public Mono<Boolean> saveFilePartBig(long fileId, int filePart, int fileTotalParts, ByteBuf bytes) {
        return Mono.defer(() -> client.sendAwait(ImmutableSaveBigFilePart.of(fileId, filePart, fileTotalParts, bytes)));
    }

    public Flux<WebFile> getWebFile(InputWebFileLocation location) {
        return Flux.defer(() -> {
            AtomicInteger offset = new AtomicInteger();
            AtomicBoolean complete = new AtomicBoolean();
            int limit = PRECISE_LIMIT;

            ImmutableGetWebFile request = ImmutableGetWebFile.of(location, 0, limit);

            return Flux.defer(() -> client.sendAwait(request.withOffset(offset.get())))
                    .mapNotNull(part -> {
                        offset.addAndGet(limit);
                        if (part.fileType() == FileType.UNKNOWN || !part.bytes().isReadable()) { // download completed
                            complete.set(true);
                            return null;
                        }

                        return part;
                    })
                    .repeat(() -> !complete.get());
        });
    }

    @BotCompatible
    public Mono<List<FileHash>> getFileHashes(InputFileLocation location, long offset) {
        return client.sendAwait(ImmutableGetFileHashes.of(location, offset));
    }
}
