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
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.ImmutableBaseInputFile;
import telegram4j.tl.ImmutableInputFileBig;
import telegram4j.tl.InputFile;
import telegram4j.tl.request.upload.GetFile;
import telegram4j.tl.request.upload.ImmutableGetWebFile;
import telegram4j.tl.request.upload.SaveBigFilePart;
import telegram4j.tl.request.upload.SaveFilePart;
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

    // TODO: bot compatible?
    public Flux<WebFile> getWebFile(FileReferenceId location) {
        return Flux.defer(() -> {
                    AtomicInteger offset = new AtomicInteger();
                    AtomicBoolean complete = new AtomicBoolean();
                    int limit = PRECISE_LIMIT;

                    var request = ImmutableGetWebFile.of(location.asWebLocation()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Default or non-proxied documents can't be downloaded as web.")),
                            0, limit);

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
                            .location(location.asLocation().orElseThrow(() -> new IllegalArgumentException(
                                    "Web documents can't be downloaded as default files")))
                            .offset(0)
                            .limit(limit)
                            .build();

                    return Flux.defer(() -> client.sendAwait(headerRequest.withOffset(offset.get())))
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
}
