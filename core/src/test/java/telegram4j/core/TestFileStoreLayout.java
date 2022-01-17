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
import telegram4j.mtproto.store.ResolvedDeletedMessages;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.store.UserNameFields;
import telegram4j.tl.*;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.help.UserInfo;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.updates.ImmutableState;
import telegram4j.tl.updates.State;
import telegram4j.tl.users.UserFull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TestFileStoreLayout implements StoreLayout {

    private static final String DB_FILE = "core/src/test/resources/db-%s.bin";

    private static final Logger log = Loggers.getLogger(TestFileStoreLayout.class);

    private final ByteBufAllocator allocator;
    private final StoreLayout delegate;

    private volatile AuthorizationKeyHolder authorizationKey;
    private volatile ImmutableState state;

    public TestFileStoreLayout(ByteBufAllocator allocator, StoreLayout delegate) {
        this.allocator = allocator;
        this.delegate = delegate;
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.justOrEmpty(authorizationKey)
                .subscribeOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.fromCallable(() -> {
                    Path fileName = Path.of(String.format(DB_FILE, dc.getId()));
                    if (!Files.exists(fileName)) {
                        return null;
                    }

                    log.debug("Loading session information from the file store for dc №{}.", dc.getId());
                    String lines = String.join("", Files.readAllLines(fileName));
                    ByteBuf buf = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(lines));
                    byte[] authKey = TlSerialUtil.deserializeBytes(buf);
                    byte[] authKeyId = TlSerialUtil.deserializeBytes(buf);
                    this.state = buf.readableBytes() == 0 ? null : TlDeserializer.deserialize(buf);

                    return new AuthorizationKeyHolder(dc, authKey, authKeyId);
                }));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        return Mono.fromRunnable(() -> this.authorizationKey = authorizationKey)
                .and(save());
    }

    private Mono<Void> save() {
        return Mono.fromCallable(() -> {
            if (authorizationKey == null) {
                return null;
            }

            log.debug("Saving session information to file store for dc №{}.", authorizationKey.getDc().getId());

            ByteBuf authKey = TlSerialUtil.serializeBytes(allocator, authorizationKey.getAuthKey());
            ByteBuf authKeyId = TlSerialUtil.serializeBytes(allocator, authorizationKey.getAuthKeyId());
            ByteBuf state = this.state != null ? TlSerializer.serialize(allocator, this.state) : Unpooled.EMPTY_BUFFER;
            ByteBuf buf = Unpooled.wrappedBuffer(authKey, authKeyId, state);

            Path fileName = Path.of(String.format(DB_FILE, authorizationKey.getDc().getId()));
            try {
                Files.write(fileName, ByteBufUtil.hexDump(buf).getBytes(StandardCharsets.UTF_8));
            } finally {
                buf.release();
            }

            return null;
        });
    }

    // Delegation of methods
    // =====================

    @Override
    public Mono<State> getCurrentState() {
        return Mono.justOrEmpty(state);
    }

    @Override
    public Mono<Long> getSelfId() {
        return delegate.getSelfId();
    }

    @Override
    public Mono<ResolvedPeer> resolvePeer(String username) {
        return delegate.resolvePeer(username);
    }

    @Override
    public Mono<InputUser> resolveUser(long userId) {
        return delegate.resolveUser(userId);
    }

    @Override
    public Mono<InputChannel> resolveChannel(long channelId) {
        return delegate.resolveChannel(channelId);
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
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessagesFields update) {
        return delegate.onDeleteMessages(update);
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
        return Mono.fromRunnable(() -> this.state = ImmutableState.copyOf(state))
                .and(save());
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
