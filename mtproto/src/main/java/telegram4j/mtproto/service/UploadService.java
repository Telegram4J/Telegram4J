package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static telegram4j.mtproto.util.EmissionHandlers.DEFAULT_PARKING;

public class UploadService extends RpcService {

    private static final int PART_SIZE = 512 * 1024;
    private static final int TEN_MB = 10 * 1024 * 1024;
    private static final int LIMIT_MB = 2000 * 1024 * 1024;
    private static final int PARALLELISM = 3;
    private static final int PRECISE_LIMIT = 1024 * 1024; // 1mb

    public UploadService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // additional methods
    // =========================
    // TODO list:
    // upload.getCdnFile#395f69da file_token:bytes offset:long limit:int = upload.CdnFile;
    // upload.reuploadCdnFile#9b2754a8 file_token:bytes request_token:bytes = Vector<FileHash>;
    // upload.getCdnFileHashes#91dc3f31 file_token:bytes offset:long = Vector<FileHash>;

    @BotCompatible
    public Mono<InputFile> saveFile(ByteBuf data, String name) {
        return Mono.defer(() -> {
            if (data.readableBytes() > LIMIT_MB) {
                return Mono.error(new IllegalArgumentException("File size is under limit. Size: "
                        + data.readableBytes() + ", limit: " + LIMIT_MB));
            }

            boolean big = data.readableBytes() > TEN_MB;
            long fileId = CryptoUtil.random.nextLong();
            int parts = (int) Math.ceil((float) data.readableBytes() / PART_SIZE);

            if (big) {
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
                                    .filter(b -> b)
                                    .switchIfEmpty(Mono.error(new IllegalStateException("Failed to upload part #" + req.filePart())))
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

                Mono<Void> reader = Flux.range(0, parts)
                        .doOnNext(filePart -> {
                            ByteBuf part = data.readSlice(Math.min(PART_SIZE, data.readableBytes()));

                            SaveBigFilePart req = SaveBigFilePart.builder()
                                    .fileId(fileId)
                                    .filePart(filePart)
                                    .bytes(part)
                                    .fileTotalParts(parts)
                                    .build();

                            queue.emitNext(req, DEFAULT_PARKING);
                        })
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

            return Flux.range(0, parts)
                    .flatMap(filePart -> {
                        ByteBuf partBytes = data.readSlice(Math.min(PART_SIZE, data.readableBytes()));

                        synchronized (md5) {
                            md5.update(partBytes.nioBuffer());
                        }

                        return client.sendAwait(ImmutableSaveFilePart.of(fileId, filePart, partBytes));
                    })
                    .then(Mono.fromSupplier(() -> ImmutableBaseInputFile.of(fileId,
                            parts, name, ByteBufUtil.hexDump(md5.digest()))));
        });
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
