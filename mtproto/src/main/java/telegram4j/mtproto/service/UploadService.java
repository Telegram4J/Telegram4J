package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.internal.EmptyArrays;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.ImmutableBaseInputFile;
import telegram4j.tl.ImmutableInputFileBig;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputWebFileLocation;
import telegram4j.tl.request.upload.GetFile;
import telegram4j.tl.request.upload.ImmutableGetWebFile;
import telegram4j.tl.request.upload.SaveBigFilePart;
import telegram4j.tl.request.upload.SaveFilePart;
import telegram4j.tl.storage.FileType;
import telegram4j.tl.upload.BaseFile;
import telegram4j.tl.upload.WebFile;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static telegram4j.mtproto.util.EmissionHandlers.DEFAULT_PARKING;

public class UploadService extends RpcService {

    private static final int PART_SIZE = 512 * 1024;
    private static final int TEN_MB = 10 * 1024 * 1024;
    private static final int LIMIT_MB = 2000 * 1024 * 1024;
    private static final int PARALLELISM = 5;
    private static final int PRECISE_LIMIT = 1024 << 10; // 1mb

    public UploadService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    @BotCompatible
    public Mono<InputFile> saveFile(ByteBuf data, String name) {
        return Mono.defer(() -> {
            long fileId = CryptoUtil.random.nextLong();
            int parts = (int) Math.ceil((float) data.readableBytes() / PART_SIZE);
            boolean big = data.readableBytes() > TEN_MB;

            if (data.readableBytes() > LIMIT_MB) {
                return Mono.error(new IllegalArgumentException("File size is under limit. Size: "
                        + data.readableBytes() + ", limit: " + LIMIT_MB));
            }

            if (big) {
                Sinks.Many<SaveBigFilePart> queue = Sinks.many().multicast()
                        .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

                AtomicInteger it = new AtomicInteger(0);
                AtomicInteger suc = new AtomicInteger(0);

                List<MTProtoClient> clients = new ArrayList<>(PARALLELISM);
                DataCenter mediaDc = DataCenter.mediaDataCentersIpv4.get(0);

                Sinks.Empty<Void> done = Sinks.empty();

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
                                            done.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
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
                            ByteBuf part = data.readBytes(Math.min(PART_SIZE, data.readableBytes()));
                            byte[] partBytes = CryptoUtil.toByteArray(part);

                            SaveBigFilePart req = SaveBigFilePart.builder()
                                    .fileId(fileId)
                                    .filePart(filePart)
                                    .bytes(partBytes)
                                    .fileTotalParts(parts)
                                    .build();

                            queue.emitNext(req, DEFAULT_PARKING);
                        })
                        .then();

                return Mono.when(initialize, reader, sender)
                        .then(Mono.fromSupplier(() -> ImmutableInputFileBig.of(fileId, parts, name)));
            }

            MessageDigest md5 = CryptoUtil.MD5.get();

            return Flux.range(0, parts)
                    .flatMap(filePart -> {
                        ByteBuf part = data.readBytes(Math.min(PART_SIZE, data.readableBytes()));
                        byte[] partBytes = CryptoUtil.toByteArray(part);

                        synchronized (md5) {
                            md5.update(partBytes);
                        }

                        SaveFilePart req = SaveFilePart.builder()
                                .fileId(fileId)
                                .filePart(filePart)
                                .bytes(partBytes)
                                .build();

                        return client.sendAwait(req);
                    })
                    .then(Mono.fromSupplier(() -> ImmutableBaseInputFile.of(fileId,
                            parts, name, ByteBufUtil.hexDump(md5.digest()))));
        });
    }

    public Flux<WebFile> getWebFile(InputWebFileLocation location) {
        return Flux.defer(() -> {
                    AtomicInteger offset = new AtomicInteger();
                    AtomicBoolean complete = new AtomicBoolean();
                    int limit = PRECISE_LIMIT;

                    return Flux.defer(() -> client.sendAwait(ImmutableGetWebFile.of(location, offset.get(), limit)))
                            .mapNotNull(part -> {
                                offset.addAndGet(limit);
                                if (part.fileType() == FileType.UNKNOWN || Arrays.equals(EmptyArrays.EMPTY_BYTES, part.bytes())) { // download completed
                                    complete.set(true);
                                    return null;
                                }

                                return part;
                            })
                            .repeat(() -> !complete.get());
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @BotCompatible
    public Flux<BaseFile> getFile(FileReferenceId location) {
        return Flux.defer(() -> {
                    AtomicInteger offset = new AtomicInteger();
                    AtomicBoolean complete = new AtomicBoolean();
                    int limit = PRECISE_LIMIT;
                    var headerRequest = GetFile.builder()
                            .precise(true)
                            // Support for downloading from CDN is probably unlikely
                            // to be implemented soon because of its complexity and pointless
                            .cdnSupported(false)
                            .location(location.asLocation())
                            .offset(offset.get())
                            .limit(limit)
                            .build();

                    return Flux.defer(() -> client.sendAwait(headerRequest.withOffset(offset.get())))
                            .cast(BaseFile.class)
                            .mapNotNull(part -> {
                                offset.addAndGet(limit);
                                if (part.type() == FileType.UNKNOWN || Arrays.equals(EmptyArrays.EMPTY_BYTES, part.bytes())) { // download completed
                                    complete.set(true);
                                    return null;
                                }

                                return part;
                            })
                            .repeat(() -> !complete.get());
                })
                .publishOn(Schedulers.boundedElastic());
    }
}
