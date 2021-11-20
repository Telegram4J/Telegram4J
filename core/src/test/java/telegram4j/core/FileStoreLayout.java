package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.store.StoreLayout;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static telegram4j.tl.TlSerialUtil.readBytes;

public class FileStoreLayout implements StoreLayout {

    private static final Path AUTH_KEY_FILE = Paths.get("core/src/test/resources/authkey.bin");

    private static final Logger log = Loggers.getLogger(FileStoreLayout.class);

    private final ByteBufAllocator allocator;

    private volatile AuthorizationKeyHolder authorizationKey;

    public FileStoreLayout(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey() {
        return Mono.justOrEmpty(authorizationKey)
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.info("Loading auth key from the file store.");

                    if (!Files.exists(AUTH_KEY_FILE)) {
                        return null;
                    }

                    String lines = String.join("", Files.readAllLines(AUTH_KEY_FILE));
                    ByteBuf buf = allocator.buffer()
                            .writeBytes(ByteBufUtil.decodeHexDump(lines));

                    int l = buf.readIntLE();
                    byte[] authKey = readBytes(buf, l);
                    int l1 = buf.readIntLE();
                    byte[] authKeyId = readBytes(buf, l1);

                    return new AuthorizationKeyHolder(authKey, authKeyId);
                }));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        return Mono.fromRunnable(() -> this.authorizationKey = authorizationKey)
                .subscribeOn(Schedulers.boundedElastic())
                .and(Mono.fromCallable(() -> {
                    log.info("Saving auth key to the file store.");

                    ByteBuf buf = allocator.buffer()
                            .writeIntLE(authorizationKey.getAuthKey().length)
                            .writeBytes(authorizationKey.getAuthKey())
                            .writeIntLE(authorizationKey.getAuthKeyId().length)
                            .writeBytes(authorizationKey.getAuthKeyId());

                    return Files.write(AUTH_KEY_FILE, ByteBufUtil.hexDump(buf).getBytes(StandardCharsets.UTF_8));
                }));
    }
}
