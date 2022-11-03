package telegram4j.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.store.MessagePoll;
import telegram4j.mtproto.store.ResolvedChatParticipant;
import telegram4j.mtproto.store.ResolvedDeletedMessages;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.channels.BaseChannelParticipants;
import telegram4j.tl.channels.ChannelParticipant;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.updates.State;
import telegram4j.tl.users.UserFull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFileStoreLayout implements StoreLayout {

    private static final String DB_FILE = "core/src/test/resources/db-%s.bin";

    private static final Logger log = Loggers.getLogger(TestFileStoreLayout.class);

    private final ByteBufAllocator allocator;
    private final StoreLayout delegate;

    public TestFileStoreLayout(StoreLayout storeLayout) {
        this(ByteBufAllocator.DEFAULT, storeLayout);
    }

    public TestFileStoreLayout(ByteBufAllocator allocator, StoreLayout delegate) {
        this.allocator = allocator;
        this.delegate = delegate;
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return delegate.getAuthorizationKey(dc)
                .publishOn(Schedulers.boundedElastic())
                .switchIfEmpty(load(dc));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(DataCenter dc, AuthorizationKeyHolder authKey) {
        return delegate.getAuthorizationKey(dc)
                .filter(authKey::equals)
                .switchIfEmpty(delegate.updateAuthorizationKey(dc, authKey)
                        .publishOn(Schedulers.boundedElastic())
                        .and(save(dc, authKey, null))
                        .then(Mono.empty()))
                .then();
    }

    @Override
    public Mono<Void> updateChannelPts(long channelId, int pts) {
        return delegate.updateChannelPts(channelId, pts);
    }

    @Override
    public Mono<Void> registerPoll(Peer peerId, int messageId, InputMediaPoll poll) {
        return delegate.registerPoll(peerId, messageId, poll);
    }

    @Override
    public Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        return delegate.onContacts(chats, users);
    }

    @Override
    public Mono<DataCenter> getDataCenter() {
        return delegate.getDataCenter();
    }

    @Override
    public Mono<State> getCurrentState() {
        return delegate.getCurrentState()
                .switchIfEmpty(loadMain().mapNotNull(p -> p.state));
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
    public Mono<InputPeer> resolvePeer(Peer peerId) {
        return delegate.resolvePeer(peerId);
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
    public Mono<Void> onNewMessage(Message update) {
        return delegate.onNewMessage(update);
    }

    @Override
    public Mono<Message> onEditMessage(Message update) {
        return delegate.onEditMessage(update);
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
    public Mono<Void> updateDataCenter(DataCenter dc) {
        return delegate.updateDataCenter(dc);
    }

    @Override
    public Mono<Void> updateState(State state) {
        return delegate.getCurrentState()
                .filter(state::equals)
                // no state info or outdated
                .switchIfEmpty(delegate.updateState(state)
                        .publishOn(Schedulers.boundedElastic())
                        .and(getDataCenter()
                                .zipWhen(this::getAuthorizationKey)
                                .flatMap(TupleUtils.function((dc, authKey) -> save(dc, authKey, state))))
                        .then(Mono.empty()))
                .then();
    }

    @Override
    public Mono<Void> onUserUpdate(telegram4j.tl.users.UserFull payload) {
        return delegate.onUserUpdate(payload);
    }

    @Override
    public Mono<Void> onChatUpdate(telegram4j.tl.messages.ChatFull payload) {
        return delegate.onChatUpdate(payload);
    }

    @Override
    public Mono<ChannelParticipant> getChannelParticipantById(long channelId, Peer peerId) {
        return delegate.getChannelParticipantById(channelId, peerId);
    }

    @Override
    public Mono<ResolvedChatParticipant> getChatParticipantById(long chatId, long userId) {
        return delegate.getChatParticipantById(chatId, userId);
    }

    @Override
    public Flux<ChannelParticipant> getChannelParticipants(long channelId) {
        return delegate.getChannelParticipants(channelId);
    }

    @Override
    public Flux<ResolvedChatParticipant> getChatParticipants(long chatId) {
        return delegate.getChatParticipants(chatId);
    }

    @Override
    public Mono<MessagePoll> getPollById(long pollId) {
        return delegate.getPollById(pollId);
    }

    @Override
    public Mono<Void> onChannelParticipants(long channelId, BaseChannelParticipants payload) {
        return delegate.onChannelParticipants(channelId, payload);
    }

    @Override
    public Mono<Void> onChannelParticipant(long channelId, ChannelParticipant payload) {
        return delegate.onChannelParticipant(channelId, payload);
    }

    @Override
    public Mono<Void> onMessages(Messages payload) {
        return delegate.onMessages(payload);
    }

    private Mono<Void> save(DataCenter dc, AuthorizationKeyHolder authKey, @Nullable State state) {
        return Mono.fromCallable(() -> {
            log.debug("Saving session information to file store for dc №{}.", dc.getId());

            ByteBuf authKeyBytes = TlSerialUtil.serializeBytes(allocator, authKey.getAuthKey().retainedDuplicate());
            ByteBuf authKeyId = TlSerialUtil.serializeBytes(allocator, authKey.getAuthKeyId().retainedDuplicate());
            ByteBuf stateBytes = state != null ? TlSerializer.serialize(allocator, state) : Unpooled.EMPTY_BUFFER;
            ByteBuf buf = Unpooled.wrappedBuffer(authKeyBytes, authKeyId, stateBytes);

            Path fileName = Path.of(String.format(DB_FILE, dc.getId()));
            try {
                Files.writeString(fileName, ByteBufUtil.hexDump(buf));
            } finally {
                buf.release();
            }

            return null;
        });
    }

    private Mono<PersistenceInfo> loadMain() {
        return getDataCenter()
                .publishOn(Schedulers.boundedElastic())
                .flatMap(dc -> {
                    Path fileName = Path.of(String.format(DB_FILE, dc.getId()));
                    if (!Files.exists(fileName)) {
                        return Mono.empty();
                    }

                    log.debug("Loading session information from the file store for main dc №{}.", dc.getId());
                    String lines;
                    try {
                        lines = Files.readString(fileName);
                    } catch (IOException e) {
                        return Mono.error(e);
                    }

                    ByteBuf buf = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(lines));
                    ByteBuf authKey = TlSerialUtil.deserializeBytes(buf).copy();
                    ByteBuf authKeyId = TlSerialUtil.deserializeBytes(buf).copy();
                    var keyHolder = new AuthorizationKeyHolder(authKey, authKeyId);
                    State state = buf.readableBytes() == 0 ? null : TlDeserializer.deserialize(buf);

                    buf.release();
                    return delegate.updateAuthorizationKey(dc, keyHolder)
                            .and(state != null ? delegate.updateState(state) : Mono.empty())
                            .thenReturn(new PersistenceInfo(keyHolder, state));
                });
    }

    private Mono<AuthorizationKeyHolder> load(DataCenter dc) {
        return Mono.defer(() -> {
            Path fileName = Path.of(String.format(DB_FILE, dc.getId()));
            if (!Files.exists(fileName)) {
                return Mono.empty();
            }

            log.debug("Loading session information from the file store for dc №{}.", dc.getId());
            String lines;
            try {
                lines = Files.readString(fileName);
            } catch (IOException e) {
                return Mono.error(e);
            }

            ByteBuf buf = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(lines));
            ByteBuf authKey = TlSerialUtil.deserializeBytes(buf).copy();
            ByteBuf authKeyId = TlSerialUtil.deserializeBytes(buf).copy();
            var keyHolder = new AuthorizationKeyHolder(authKey, authKeyId);
            buf.release();

            return delegate.updateAuthorizationKey(dc, keyHolder)
                    .thenReturn(keyHolder);
        });
    }

    static class PersistenceInfo {
        final AuthorizationKeyHolder authKey;
        @Nullable
        final State state;

        PersistenceInfo(AuthorizationKeyHolder authKey, @Nullable State state) {
            this.authKey = authKey;
            this.state = state;
        }
    }
}
