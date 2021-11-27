package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static telegram4j.tl.TlSerialUtil.readBytes;

public class FileStoreLayout implements StoreLayout {

    private static final String AUTH_KEY_FILE = "core/src/test/resources/authkey-%s.bin";

    private static final Logger log = Loggers.getLogger(FileStoreLayout.class);

    private final ByteBufAllocator allocator;
    private final StoreLayout delegate;

    private volatile AuthorizationKeyHolder authorizationKey;

    public FileStoreLayout(ByteBufAllocator allocator, StoreLayout delegate) {
        this.allocator = allocator;
        this.delegate = delegate;
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.justOrEmpty(authorizationKey)
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.info("Loading auth key from the file store for dc №{}.", dc.getId());

                    Path fileName = Paths.get(String.format(AUTH_KEY_FILE, dc.getId()));
                    if (!Files.exists(fileName)) {
                        return null;
                    }

                    String lines = String.join("", Files.readAllLines(fileName));
                    ByteBuf buf = allocator.buffer()
                            .writeBytes(ByteBufUtil.decodeHexDump(lines));

                    int l = buf.readIntLE();
                    byte[] authKey = readBytes(buf, l);
                    int l1 = buf.readIntLE();
                    byte[] authKeyId = readBytes(buf, l1);

                    return new AuthorizationKeyHolder(dc, authKey, authKeyId);
                }));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        return Mono.fromRunnable(() -> this.authorizationKey = authorizationKey)
                .subscribeOn(Schedulers.boundedElastic())
                .and(Mono.fromCallable(() -> {
                    log.info("Saving auth key to the file store for dc №{}.", authorizationKey.getDc().getId());

                    ByteBuf buf = allocator.buffer()
                            .writeIntLE(authorizationKey.getAuthKey().length)
                            .writeBytes(authorizationKey.getAuthKey())
                            .writeIntLE(authorizationKey.getAuthKeyId().length)
                            .writeBytes(authorizationKey.getAuthKeyId());

                    Path fileName = Paths.get(String.format(AUTH_KEY_FILE, authorizationKey.getDc().getId()));
                    return Files.write(fileName, ByteBufUtil.hexDump(buf).getBytes(StandardCharsets.UTF_8));
                }));
    }

    // Delegation of methods
    // =====================

    @Override
    public Mono<Long> getSelfId() {
        return delegate.getSelfId();
    }

    @Override
    public Mono<InputPeer> resolvePeer(String username) {
        return delegate.resolvePeer(username);
    }

    @Override
    public Mono<Message> getMessageById(long chatId, int messageId) {
        return delegate.getMessageById(chatId, messageId);
    }

    @Override
    public Mono<Chat> getChatById(long chatId) {
        return delegate.getChatById(chatId);
    }

    @Override
    public Mono<User> getUserById(long userId) {
        return delegate.getUserById(userId);
    }

    @Override
    public Mono<Void> updateSelfId(long userId) {
        return delegate.updateSelfId(userId);
    }

    @Override
    public Mono<Void> onNewMessage(UpdateNewMessage update, List<Chat> chats, List<User> users) {
        return delegate.onNewMessage(update, chats, users);
    }
}
