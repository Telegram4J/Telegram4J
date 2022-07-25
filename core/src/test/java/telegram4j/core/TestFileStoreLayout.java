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
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.updates.ImmutableState;
import telegram4j.tl.updates.State;
import telegram4j.tl.users.UserFull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFileStoreLayout implements StoreLayout {

    private static final String DB_FILE = "core/src/test/resources/db-%s.bin";

    private static final Logger log = Loggers.getLogger(TestFileStoreLayout.class);

    private final ByteBufAllocator allocator;
    private final StoreLayout delegate;

    private volatile AuthorizationKeyHolder authKey;
    private volatile ImmutableState state;

    public TestFileStoreLayout(StoreLayout storeLayout) {
        this(ByteBufAllocator.DEFAULT, storeLayout);
    }

    public TestFileStoreLayout(ByteBufAllocator allocator, StoreLayout delegate) {
        this.allocator = allocator;
        this.delegate = delegate;
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.fromSupplier(() -> authKey)
                .publishOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.fromCallable(() -> {
                    Path fileName = Path.of(String.format(DB_FILE, dc.getId()));
                    if (!Files.exists(fileName)) {
                        return null;
                    }

                    log.debug("Loading session information from the file store for dc №{}.", dc.getId());
                    String lines = String.join("", Files.readAllLines(fileName));
                    ByteBuf buf = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(lines));
                    ByteBuf authKey = TlSerialUtil.deserializeBytes(buf).copy();
                    ByteBuf authKeyId = TlSerialUtil.deserializeBytes(buf).copy();
                    this.state = buf.readableBytes() == 0 ? null : TlDeserializer.deserialize(buf);
                    buf.release();

                    var holder = new AuthorizationKeyHolder(dc, authKey, authKeyId);
                    this.authKey = holder;
                    return holder;
                }));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authKey) {
        return Mono.defer(() -> {
            if (!authKey.equals(this.authKey)) {
                this.authKey = authKey;
                return save();
            }
            return Mono.empty();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        return delegate.onContacts(chats, users);
    }

    private Mono<Void> save() {
        return Mono.fromCallable(() -> {
            var key = authKey;
            if (key == null) {
                return null;
            }

            log.debug("Saving session information to file store for dc №{}.", key.getDc().getId());

            ByteBuf authKey = TlSerialUtil.serializeBytes(allocator, key.getAuthKey().retainedDuplicate());
            ByteBuf authKeyId = TlSerialUtil.serializeBytes(allocator, key.getAuthKeyId().retainedDuplicate());
            ByteBuf state = this.state != null ? TlSerializer.serialize(allocator, this.state) : Unpooled.EMPTY_BUFFER;
            ByteBuf buf = Unpooled.wrappedBuffer(authKey, authKeyId, state);

            Path fileName = Path.of(String.format(DB_FILE, key.getDc().getId()));
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
        return Mono.fromSupplier(() -> state);
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
    public Mono<Boolean> existMessage(BaseMessageFields message) {
        return delegate.existMessage(message);
    }

    @Override
    public Mono<Messages> getMessages(Iterable<? extends InputMessage> messageIds) {
        return delegate.getMessages(messageIds);
    }

    @Override
    public Mono<Messages> getMessages(long channelId, Iterable<? extends InputMessage> messageIds) {
        return delegate.getMessages(channelId, messageIds);
    }

    @Override
    public Mono<BaseChat> getChatMinById(long chatId) {
        return delegate.getChatMinById(chatId);
    }

    @Override
    public Mono<ChatFull> getChatFullById(long chatId) {
        return delegate.getChatFullById(chatId);
    }

    @Override
    public Mono<Channel> getChannelMinById(long channelId) {
        return delegate.getChannelMinById(channelId);
    }

    @Override
    public Mono<ChatFull> getChannelFullById(long channelId) {
        return delegate.getChannelFullById(channelId);
    }

    @Override
    public Mono<BaseUser> getUserMinById(long userId) {
        return delegate.getUserMinById(userId);
    }

    @Override
    public Mono<UserFull> getUserFullById(long userId) {
        return delegate.getUserFullById(userId);
    }

    @Override
    public Mono<Void> onNewMessage(Message message) {
        return delegate.onNewMessage(message);
    }

    @Override
    public Mono<Message> onEditMessage(Message message) {
        return delegate.onEditMessage(message);
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessagesFields update) {
        return delegate.onDeleteMessages(update);
    }

    @Override
    public Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessagesFields payload) {
        return delegate.onUpdatePinnedMessages(payload);
    }

    @Override
    public Mono<Void> onChannelUserTyping(UpdateChannelUserTyping payload) {
        return delegate.onChannelUserTyping(payload);
    }

    @Override
    public Mono<Void> onChatUserTyping(UpdateChatUserTyping payload) {
        return delegate.onChatUserTyping(payload);
    }

    @Override
    public Mono<Void> onUserTyping(UpdateUserTyping payload) {
        return delegate.onUserTyping(payload);
    }

    @Override
    public Mono<UserNameFields> onUserNameUpdate(UpdateUserName payload) {
        return delegate.onUserNameUpdate(payload);
    }

    @Override
    public Mono<String> onUserPhoneUpdate(UpdateUserPhone payload) {
        return delegate.onUserPhoneUpdate(payload);
    }

    @Override
    public Mono<UserProfilePhoto> onUserPhotoUpdate(UpdateUserPhoto payload) {
        return delegate.onUserPhotoUpdate(payload);
    }

    @Override
    public Mono<UserStatus> onUserStatusUpdate(UpdateUserStatus payload) {
        return delegate.onUserStatusUpdate(payload);
    }

    @Override
    public Mono<Void> onChatParticipantAdd(UpdateChatParticipantAdd payload) {
        return delegate.onChatParticipantAdd(payload);
    }

    @Override
    public Mono<Void> onChatParticipantAdmin(UpdateChatParticipantAdmin payload) {
        return delegate.onChatParticipantAdmin(payload);
    }

    @Override
    public Mono<Void> onChatParticipantDelete(UpdateChatParticipantDelete payload) {
        return delegate.onChatParticipantDelete(payload);
    }

    @Override
    public Mono<Void> onChatParticipant(UpdateChatParticipant payload) {
        return delegate.onChatParticipant(payload);
    }

    @Override
    public Mono<Void> onChatParticipants(ChatParticipants payload) {
        return delegate.onChatParticipants(payload);
    }

    @Override
    public Mono<Void> onChannelParticipant(UpdateChannelParticipant payload) {
        return delegate.onChannelParticipant(payload);
    }

    @Override
    public Mono<Void> updateSelfId(long userId) {
        return delegate.updateSelfId(userId);
    }

    @Override
    public Mono<Void> updateState(State state) {
        return Mono.defer(() -> {
            if (!state.equals(this.state)) {
                this.state = ImmutableState.copyOf(state);
                return save();
            }
            return Mono.empty();
        })
        .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> onUserUpdate(telegram4j.tl.users.UserFull payload) {
        return delegate.onUserUpdate(payload);
    }

    @Override
    public Mono<Void> onUserUpdate(User payload) {
        return delegate.onUserUpdate(payload);
    }

    @Override
    public Mono<Void> onChatUpdate(Chat payload) {
        return delegate.onChatUpdate(payload);
    }

    @Override
    public Mono<Void> onChatUpdate(telegram4j.tl.messages.ChatFull payload) {
        return delegate.onChatUpdate(payload);
    }

    @Override
    public Mono<Void> onResolvedPeer(ResolvedPeer payload) {
        return delegate.onResolvedPeer(payload);
    }
}
