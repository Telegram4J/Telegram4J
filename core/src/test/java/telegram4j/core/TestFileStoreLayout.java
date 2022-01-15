package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.store.UserNameFields;
import telegram4j.tl.*;
import telegram4j.tl.help.UserInfo;
import telegram4j.tl.updates.State;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static telegram4j.tl.TlSerialUtil.readBytes;

public class TestFileStoreLayout implements StoreLayout {

    private static final String AUTH_KEY_FILE = "core/src/test/resources/authkey-%s.bin";

    private static final Logger log = Loggers.getLogger(TestFileStoreLayout.class);

    private final ByteBufAllocator allocator;
    private final StoreLayout delegate;

    private volatile AuthorizationKeyHolder authorizationKey;

    public TestFileStoreLayout(ByteBufAllocator allocator, StoreLayout delegate) {
        this.allocator = allocator;
        this.delegate = delegate;
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.justOrEmpty(authorizationKey)
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.fromCallable(() -> {
                    Path fileName = Path.of(String.format(AUTH_KEY_FILE, dc.getId()));
                    if (!Files.exists(fileName)) {
                        return null;
                    }

                    log.info("Loading auth key from the file store for dc №{}.", dc.getId());
                    String lines = String.join("", Files.readAllLines(fileName));
                    ByteBuf buf = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(lines));
                    int l = buf.readIntLE();
                    byte[] authKey = readBytes(buf, l);
                    int l1 = buf.readIntLE();
                    byte[] authKeyId = readBytes(buf, l1);
                    buf.release();

                    return new AuthorizationKeyHolder(dc, authKey, authKeyId);
                }));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        return Mono.fromRunnable(() -> this.authorizationKey = authorizationKey)
                .subscribeOn(Schedulers.boundedElastic())
                .and(Mono.fromCallable(() -> {
                    log.info("Saving auth key to the file store for dc №{}.", authorizationKey.getDc().getId());

                    byte[] authKey = authorizationKey.getAuthKey();
                    byte[] authKeyId = authorizationKey.getAuthKeyId();
                    ByteBuf buf = allocator.buffer(8 + authKey.length + authKeyId.length)
                            .writeIntLE(authKey.length)
                            .writeBytes(authKey)
                            .writeIntLE(authKeyId.length)
                            .writeBytes(authKeyId);

                    Path fileName = Path.of(String.format(AUTH_KEY_FILE, authorizationKey.getDc().getId()));
                    try {
                        return Files.write(fileName, ByteBufUtil.hexDump(buf).getBytes(StandardCharsets.UTF_8));
                    } finally {
                        buf.release();
                    }
                }));
    }

    // Delegation of methods
    // =====================

    @Override
    public Mono<State> getCurrentState() {
        return delegate.getCurrentState();
    }

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
    public Mono<Chat> getChatMinById(long chatId) {
        return delegate.getChatMinById(chatId);
    }

    @Override
    public Mono<ChatFull> getChatFullById(long chatId) {
        return delegate.getChatFullById(chatId);
    }

    @Override
    public Mono<User> getUserMinById(long userId) {
        return delegate.getUserMinById(userId);
    }

    @Override
    public Mono<UserFull> getUserFullById(long userId) {
        return delegate.getUserFullById(userId);
    }

    @Override
    public Mono<Void> onNewMessage(Message message, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onNewMessage(message, chats, users);
    }

    @Override
    public Mono<Message> onEditMessage(Message message, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onEditMessage(message, chats, users);
    }

    @Override
    public Mono<Void> onChannelUserTyping(UpdateChannelUserTyping action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChannelUserTyping(action, chats, users);
    }

    @Override
    public Mono<Void> onChatUserTyping(UpdateChatUserTyping action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChatUserTyping(action, chats, users);
    }

    @Override
    public Mono<Void> onUserTyping(UpdateUserTyping action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onUserTyping(action, chats, users);
    }

    @Override
    public Mono<UserNameFields> onUserNameUpdate(UpdateUserName action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onUserNameUpdate(action, chats, users);
    }

    @Override
    public Mono<String> onUserPhoneUpdate(UpdateUserPhone action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onUserPhoneUpdate(action, chats, users);
    }

    @Override
    public Mono<UserProfilePhoto> onUserPhotoUpdate(UpdateUserPhoto action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onUserPhotoUpdate(action, chats, users);
    }

    @Override
    public Mono<UserStatus> onUserStatusUpdate(UpdateUserStatus action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onUserStatusUpdate(action, chats, users);
    }

    @Override
    public Mono<Void> onChatParticipantAdd(UpdateChatParticipantAdd action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChatParticipantAdd(action, chats, users);
    }

    @Override
    public Mono<Void> onChatParticipantAdmin(UpdateChatParticipantAdmin action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChatParticipantAdmin(action, chats, users);
    }

    @Override
    public Mono<Void> onChatParticipantDelete(UpdateChatParticipantDelete action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChatParticipantDelete(action, chats, users);
    }

    @Override
    public Mono<Void> onChatParticipant(UpdateChatParticipant action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChatParticipant(action, chats, users);
    }

    @Override
    public Mono<Void> onChatParticipants(UpdateChatParticipants action, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChatParticipants(action, chats, users);
    }

    @Override
    public Mono<Void> onChannelParticipant(UpdateChannelParticipant update, Map<Long, Chat> chats, Map<Long, User> users) {
        return delegate.onChannelParticipant(update, chats, users);
    }

    @Override
    public Mono<Void> updateSelfId(long userId) {
        return delegate.updateSelfId(userId);
    }

    @Override
    public Mono<Void> updateState(State state) {
        return delegate.updateState(state);
    }

    @Override
    public Mono<telegram4j.tl.users.UserFull> onUserUpdate(telegram4j.tl.users.UserFull payload) {
        return delegate.onUserUpdate(payload);
    }

    @Override
    public Mono<User> onUserUpdate(User payload) {
        return delegate.onUserUpdate(payload);
    }

    @Override
    public Mono<UserInfo> onUserInfoUpdate(UserInfo payload) {
        return delegate.onUserInfoUpdate(payload);
    }

    @Override
    public Mono<Chat> onChatUpdate(Chat payload) {
        return delegate.onChatUpdate(payload);
    }

    @Override
    public Mono<telegram4j.tl.messages.ChatFull> onChatUpdate(telegram4j.tl.messages.ChatFull payload) {
        return delegate.onChatUpdate(payload);
    }
}
