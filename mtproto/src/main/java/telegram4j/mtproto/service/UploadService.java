package telegram4j.mtproto.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.service.Compatible.Type;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.FileHash;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputFileLocation;
import telegram4j.tl.InputWebFileLocation;
import telegram4j.tl.request.upload.ImmutableGetFile;
import telegram4j.tl.request.upload.ImmutableGetFileHashes;
import telegram4j.tl.request.upload.ImmutableGetWebFile;
import telegram4j.tl.storage.FileType;
import telegram4j.tl.upload.BaseFile;
import telegram4j.tl.upload.WebFile;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UploadService extends RpcService {
    static final Logger log = Loggers.getLogger(UploadService.class);

    public static final int MIN_PART_SIZE = 1024;
    public static final int MAX_PART_SIZE = 512 * 1024;

    public static final int PRECISE_LIMIT = 1024 * 1024;
    public static final int BIG_FILE_THRESHOLD = 10 * 1024 * 1024;

    static final int MAX_PARTS_COUNT = 4000; // it's for users with tg premium; for other users limit is 3000
    static final long MAX_FILE_SIZE = 4L * 1000 * 1024 * 1024; // 4gb for premium users; for other = 2gb

    public UploadService(MTProtoClientGroup groupManager, StoreLayout storeLayout) {
        super(groupManager, storeLayout);
    }

    public static int suggestParallelism(long size) {
        return size > BIG_FILE_THRESHOLD ? 3 : 1;
    }

    public static int suggestPartSize(long size, int partSize) {
        if (partSize == -1) {
            return MAX_PART_SIZE; // TODO: adaptive part size
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

    @Compatible(Type.BOTH)
    public Mono<InputFile> saveFile(UploadOptions options) {
        Objects.requireNonNull(options);

        return new UploadMono(groupManager, options);
    }

    // upload namespace
    // =========================

    @Compatible(Type.BOTH)
    public Flux<BaseFile> getFile(InputFileLocation location) {
        return Flux.defer(() -> {
            AtomicInteger offset = new AtomicInteger();
            AtomicBoolean complete = new AtomicBoolean();
            int limit = PRECISE_LIMIT;
            ImmutableGetFile request = ImmutableGetFile.of(ImmutableGetFile.PRECISE_MASK, location, 0, limit);

            return Flux.defer(() -> sendMain(request.withOffset(offset.get())))
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

    public Flux<WebFile> getWebFile(InputWebFileLocation location) {
        return Flux.defer(() -> {
            AtomicInteger offset = new AtomicInteger();
            AtomicBoolean complete = new AtomicBoolean();
            int limit = PRECISE_LIMIT;

            ImmutableGetWebFile request = ImmutableGetWebFile.of(location, 0, limit);

            return Flux.defer(() -> sendMain(request.withOffset(offset.get())))
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

    @Compatible(Type.BOTH)
    public Mono<List<FileHash>> getFileHashes(InputFileLocation location, long offset) {
        return sendMain(ImmutableGetFileHashes.of(location, offset));
    }
}
