package telegram4j.mtproto.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.BaseMessages;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.updates.ImmutableState;
import telegram4j.tl.updates.State;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;
import static telegram4j.mtproto.util.TlEntityUtil.stripUsername;

public class StoreLayoutImpl implements StoreLayout {

    private final Cache<MessageId, BaseMessageFields> messages;
    private final ConcurrentMap<Long, PartialFields<Chat, ChatFull>> chats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PartialFields<ImmutableBaseUser, ImmutableUserFull>> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, InputPeer> usernames = new ConcurrentHashMap<>();
    private final ConcurrentMap<DataCenter, AuthorizationKeyHolder> authorizationKeys = new ConcurrentHashMap<>();

    private volatile long selfId;
    private volatile ImmutableState state;

    public StoreLayoutImpl(Function<Caffeine<Object, Object>, Caffeine<Object, Object>> cacheFactory) {
        this.messages = cacheFactory.apply(Caffeine.newBuilder()).build();
    }

    @Override
    public Mono<State> getCurrentState() {
        return Mono.justOrEmpty(state);
    }

    @Override
    public Mono<Long> getSelfId() {
        return Mono.justOrEmpty(selfId);
    }

    @Override
    public Mono<ResolvedPeer> resolvePeer(String username) {
        return Mono.fromSupplier(() -> usernames.get(stripUsername(username)))
                .mapNotNull(p -> {
                    Peer peer = asPeer(p);
                    List<User> user = new ArrayList<>(1);
                    List<Chat> chat = new ArrayList<>(1);
                    addContact(peer, chat, user);

                    return ResolvedPeer.builder()
                            .users(user)
                            .chats(chat)
                            .peer(peer)
                            .build();
                });
    }

    @Override
    public Mono<InputUser> resolveUser(long userId) {
        return Mono.fromSupplier(() -> {
            if (userId == selfId) {
                return InputUserSelf.instance();
            }

            return Optional.ofNullable(chats.get(userId))
                    .map(c -> (Channel) c.min)
                    .map(Channel::accessHash)
                    .map(ah -> ImmutableBaseInputUser.of(userId, ah))
                    .orElse(null);
        });
    }

    @Override
    public Mono<InputChannel> resolveChannel(long channelId) {
        return Mono.fromSupplier(() -> Optional.ofNullable(chats.get(channelId))
                .map(c -> (Channel) c.min)
                .map(Channel::accessHash)
                .map(ah -> ImmutableBaseInputChannel.of(channelId, ah))
                .orElse(null));
    }

    @Override
    public Mono<Boolean> existMessage(BaseMessageFields message) {
        return Mono.fromSupplier(() -> messages.getIfPresent(MessageId.create(message)) != null);
    }

    @Override
    public Mono<Messages> getMessages(Iterable<? extends InputMessage> messageIds) {
        return Mono.fromSupplier(() -> {
            var ids = StreamSupport.stream(messageIds.spliterator(), false)
                    // Search must match what the server gives out,
                    // and the server does not return anything when sending this id.
                    .filter(i -> i.identifier() != InputMessagePinned.ID)
                    .map(id -> {
                        switch (id.identifier()) {
                            case InputMessageID.ID: return ((InputMessageID) id).id();
                            case InputMessageReplyTo.ID: return ((InputMessageReplyTo) id).id();
                            case InputMessageCallbackQuery.ID:
                                // TODO
                            default:
                                throw new IllegalArgumentException("Unknown message id type: " + id);
                        }
                    })
                    .map(i -> new MessageId(i, -1))
                    .collect(Collectors.toSet());

            var messages = this.messages.getAllPresent(ids).values();

            Set<User> users = new HashSet<>();
            Set<Chat> chats = new HashSet<>();
            for (var message : messages) {
                addContact(message.peerId(), chats, users);
                Peer fromId = message.fromId();
                if (fromId != null) {
                    addContact(fromId, chats, users);
                }
            }

            return BaseMessages.builder()
                    .messages(messages)
                    .chats(chats)
                    .users(users)
                    .build();
        });
    }

    @Override
    public Mono<Messages> getMessages(long channelId, Iterable<? extends InputMessage> messageIds) {
        return Mono.fromSupplier(() -> {
            var ids = StreamSupport.stream(messageIds.spliterator(), false)
                    .map(id -> {
                        switch (id.identifier()) {
                            case InputMessageID.ID: return ((InputMessageID) id).id();
                            case InputMessageReplyTo.ID: return ((InputMessageReplyTo) id).id();
                            case InputMessagePinned.ID:
                                return Optional.ofNullable(this.chats.get(channelId))
                                        .map(PartialFields::getFull)
                                        // TODO: why parser doesnt pull up #pinnedMsgId method?
                                        .map(c -> c.identifier() == ChannelFull.ID
                                                ? ((ChannelFull) c).pinnedMsgId()
                                                : ((BaseChatFull) c).pinnedMsgId())
                                        .orElse(null);
                            case InputMessageCallbackQuery.ID:
                                // TODO
                            default:
                                throw new IllegalArgumentException("Unknown message id type: " + id);
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(i -> new MessageId(i, channelId))
                    .collect(Collectors.toSet());

            var messages = this.messages.getAllPresent(ids).values();

            Set<User> users = new HashSet<>();
            Set<Chat> chats = new HashSet<>();
            for (var message : messages) {
                addContact(message.peerId(), chats, users);
                Peer fromId = message.fromId();
                if (fromId != null) {
                    addContact(fromId, chats, users);
                }
            }

            return BaseMessages.builder()
                    .messages(messages)
                    .chats(chats)
                    .users(users)
                    .build();
        });
    }

    @Override
    public Mono<Chat> getChatMinById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId)).map(PartialFields::getMin);
    }

    @Override
    public Mono<telegram4j.tl.messages.ChatFull> getChatFullById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId))
                .filter(userInfo -> userInfo.full != null)
                .map(userInfo -> telegram4j.tl.messages.ChatFull.builder()
                        .chats(List.of(userInfo.min))
                        .fullChat(Objects.requireNonNull(userInfo.full))
                        .build());
    }

    @Override
    public Mono<User> getUserMinById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId)).map(PartialFields::getMin);
    }

    @Override
    public Mono<telegram4j.tl.users.UserFull> getUserFullById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId))
                .filter(userInfo -> userInfo.full != null)
                .map(userInfo -> telegram4j.tl.users.UserFull.builder()
                        .addUser(userInfo.min)
                        .fullUser(Objects.requireNonNull(userInfo.full))
                        .build());
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.fromSupplier(() -> authorizationKeys.get(dc));
    }

    // Updates methods
    // ==================

    @Override
    public Mono<Void> onNewMessage(Message message) {
        return Mono.fromRunnable(() -> {
            BaseMessageFields cast = (BaseMessageFields) message;
            MessageId key = MessageId.create(cast);

            messages.put(key, cast);
        });
    }

    @Override
    public Mono<Message> onEditMessage(Message message) {
        return Mono.fromSupplier(() -> {
            BaseMessageFields cast = (BaseMessageFields) message;
            MessageId key = MessageId.create(cast);

            return messages.asMap().put(key, cast);
        });
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessagesFields update) {
        return Mono.fromSupplier(() -> {

            InputPeer peer;
            switch (update.identifier()) {
                case UpdateDeleteChannelMessages.ID:
                    long channelId = ((UpdateDeleteChannelMessages) update).channelId();
                    var channelInfo = chats.get(channelId);
                    if (channelInfo == null) {
                        peer = InputPeerEmpty.instance();
                        break;
                    }

                    Channel c = (Channel) channelInfo.min;
                    long accessHash = Objects.requireNonNull(c.accessHash());
                    peer = ImmutableInputPeerChannel.of(channelId, accessHash);
                    break;
                case UpdateDeleteScheduledMessages.ID:
                    Peer p = ((UpdateDeleteScheduledMessages) update).peer();
                    peer = getInputPeer(p);
                    break;
                case UpdateDeleteMessages.ID:
                    peer = update.messages().stream()
                            .map(i -> new MessageId(i, -1))
                            .map(messages.asMap()::remove)
                            .filter(Objects::nonNull)
                            .map(m -> getInputPeer(m.peerId()))
                            .findFirst()
                            .orElse(InputPeerEmpty.instance());
                    break;
                default:
                    throw new IllegalStateException();
            }

            long rawPeerId;
            switch (peer.identifier()) {
                case InputPeerEmpty.ID:
                    return null;
                case InputPeerChannel.ID:
                case InputPeerChannelFromMessage.ID:
                    rawPeerId = getRawInputPeerId(peer);
                    break;
                case InputPeerChat.ID:
                case InputPeerSelf.ID:
                case InputPeerUser.ID:
                case InputPeerUserFromMessage.ID:
                    rawPeerId = -1;
                    break;
                default:
                    throw new IllegalStateException();
            }

            var messages = update.messages().stream()
                    .map(id -> new MessageId(id, rawPeerId))
                    .map(this.messages.asMap()::remove)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return new ResolvedDeletedMessages(peer, messages);
        });
    }

    @Override
    public Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessagesFields payload) {
        return Mono.fromRunnable(() -> {

            long chatId = payload.identifier() == UpdatePinnedChannelMessages.ID
                    ? ((UpdatePinnedChannelMessages) payload).channelId() : -1;

            payload.messages().stream()
                    .map(i -> new MessageId(i, chatId))
                    .forEach(k -> messages.asMap().computeIfPresent(k, (k1, v) -> {
                        if (v.identifier() == BaseMessage.ID) {
                            return ImmutableBaseMessage.copyOf((BaseMessage) v)
                                    .withPinned(payload.pinned());
                        }
                        return v;
                    }));
        });
    }

    @Override
    public Mono<Void> onChannelUserTyping(UpdateChannelUserTyping action) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatUserTyping(UpdateChatUserTyping action) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onUserTyping(UpdateUserTyping action) {
        return Mono.empty();
    }

    @Override
    public Mono<UserNameFields> onUserNameUpdate(UpdateUserName action) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(action.userId());
            UserNameFields fields = old != null ? new UserNameFields(old.min.username(), old.min.firstName(), old.min.lastName()) : null;
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min
                    .withUsername(action.username())
                    .withFirstName(action.firstName())
                    .withLastName(action.lastName())));

            return fields;
        });
    }

    @Override
    public Mono<String> onUserPhoneUpdate(UpdateUserPhone action) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(action.userId());
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min.withPhone(action.phone())));

            return old != null ? old.min.phone() : null;
        });
    }

    @Override
    public Mono<UserProfilePhoto> onUserPhotoUpdate(UpdateUserPhoto action) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(action.userId());
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min.withPhoto(action.photo())));

            return old != null ? old.min.photo() : null;
        });
    }

    @Override
    public Mono<UserStatus> onUserStatusUpdate(UpdateUserStatus action) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(action.userId());
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min.withStatus(action.status())));

            return old != null ? old.min.status() : null;
        });
    }

    @Override
    public Mono<Void> onChatParticipantAdd(UpdateChatParticipantAdd action) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipantAdmin(UpdateChatParticipantAdmin action) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipantDelete(UpdateChatParticipantDelete action) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipant(UpdateChatParticipant action) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipants(UpdateChatParticipants action) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChannelParticipant(UpdateChannelParticipant update) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> updateSelfId(long userId) {
        return Mono.fromRunnable(() -> selfId = userId);
    }

    @Override
    public Mono<Void> updateState(State state) {
        return Mono.fromRunnable(() -> this.state = ImmutableState.copyOf(state));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        return Mono.fromRunnable(() -> authorizationKeys.put(authorizationKey.getDc(), authorizationKey));
    }

    @Override
    public Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        return Mono.fromRunnable(() -> saveContacts(chats, users));
    }

    @Override
    public Mono<User> onUserUpdate(User payload) {
        return Mono.fromSupplier(() -> saveUserMin(payload)).mapNotNull(PartialFields::getMin);
    }

    @Override
    public Mono<telegram4j.tl.users.UserFull> onUserUpdate(telegram4j.tl.users.UserFull payload) {
        return Mono.fromSupplier(() -> saveUserFull(payload))
                .filter(userInfo -> userInfo.full != null)
                .map(userInfo -> telegram4j.tl.users.UserFull.builder()
                        .users(List.of(userInfo.min))
                        .fullUser(Objects.requireNonNull(userInfo.full))
                        .build());
    }

    @Override
    public Mono<Chat> onChatUpdate(Chat payload) {
        return Mono.fromSupplier(() -> saveChatMin(payload)).mapNotNull(PartialFields::getMin);
    }

    @Override
    public Mono<telegram4j.tl.messages.ChatFull> onChatUpdate(telegram4j.tl.messages.ChatFull payload) {
        return Mono.fromSupplier(() -> saveChatFull(payload))
                .filter(chatInfo -> chatInfo.full != null)
                .map(chatInfo -> telegram4j.tl.messages.ChatFull.builder()
                        .chats(List.of(chatInfo.min))
                        .fullChat(Objects.requireNonNull(chatInfo.full))
                        .build());
    }

    @Override
    public Mono<ResolvedPeer> onResolvedPeer(ResolvedPeer payload) {
        return Mono.fromSupplier(() -> {
            long rawPeerId = getRawPeerId(payload.peer());
            var old = ResolvedPeer.builder()
                    .peer(payload.peer());
            switch (payload.peer().identifier()) {
                case PeerChannel.ID:
                case PeerChat.ID:
                    var chatInfo = chats.get(rawPeerId);
                    if (chatInfo == null) {
                        return null;
                    }

                    old.addChat(chatInfo.min);
                    break;
                case PeerUser.ID:
                    var userInfo = users.get(rawPeerId);
                    if (userInfo == null) {
                        return null;
                    }

                    old.addUser(userInfo.min);
                    break;
                default: throw new IllegalStateException();
            }

            saveContacts(payload.chats(), payload.users());
            return old.build();
        });
    }

    @Nullable
    private PartialFields<Chat, ChatFull> saveChatFull(telegram4j.tl.messages.ChatFull chat) {
        ChatFull chat0 = copy(chat.fullChat());
        Chat chat1 = copy(chat.chats().stream()
                .filter(c -> isAccessible(c) && c.id() == chat0.id())
                .findFirst()
                .orElseThrow());

        if (!isAccessible(chat1)) {
            return null;
        }

        var old = chats.computeIfAbsent(chat0.id(), k -> new PartialFields<>(chat1, chat0));
        chats.computeIfPresent(chat0.id(), (k, v) -> new PartialFields<>(chat1, chat0));
        saveUsernamePeer(chat0);

        return old;
    }

    @Nullable
    private PartialFields<ImmutableBaseUser, ImmutableUserFull> saveUserFull(telegram4j.tl.users.UserFull user) {
        ImmutableUserFull user0 = ImmutableUserFull.copyOf(user.fullUser());
        ImmutableBaseUser user1 = ImmutableBaseUser.copyOf(user.users().stream()
                .filter(u -> u.identifier() == BaseUser.ID && u.id() == user0.id())
                .map(u -> (BaseUser) u)
                .findFirst()
                .orElseThrow());

        if (!isAccessible(user1)) {
            return null;
        }

        var old = users.computeIfAbsent(user0.id(), k -> new PartialFields<>(user1, user0));
        users.computeIfPresent(user0.id(), (k, v) -> new PartialFields<>(user1, user0));
        saveUsernamePeer(user1);

        return old;
    }

    @Nullable
    private PartialFields<ImmutableBaseUser, ImmutableUserFull> saveUserMin(User user) {
        if (user.identifier() != BaseUser.ID || !isAccessible(user)) {
            return null;
        }

        ImmutableBaseUser user0 = ImmutableBaseUser.copyOf((BaseUser) user);
        var old = users.computeIfAbsent(user.id(), k -> new PartialFields<>(user0));
        users.computeIfPresent(user0.id(), (k, v) -> new PartialFields<>(user0, v.full));
        saveUsernamePeer(user0);

        return old;
    }

    private InputPeer getInputPeer(Peer peer) {
        long rawPeerId = TlEntityUtil.getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerChat.ID:
                var chatInfo = chats.get(rawPeerId);
                if (chatInfo == null) {
                    return InputPeerEmpty.instance();
                }

                return ImmutableInputPeerChat.of(rawPeerId);
            case PeerChannel.ID: {
                var channelInfo = chats.get(rawPeerId);
                if (channelInfo == null) {
                    return InputPeerEmpty.instance();
                }

                Channel channel = (Channel) channelInfo.min;
                long accessHash = Objects.requireNonNull(channel.accessHash());
                return ImmutableInputPeerChannel.of(rawPeerId, accessHash);
            }
            case PeerUser.ID:
                if (rawPeerId == selfId) {
                    return InputPeerSelf.instance();
                }

                var userInfo = users.get(rawPeerId);
                if (userInfo == null) {
                    return InputPeerEmpty.instance();
                }

                long accessHash = Objects.requireNonNull(userInfo.min.accessHash());
                return ImmutableInputPeerUser.of(userInfo.min.id(), accessHash);
            default:
                throw new IllegalStateException();
        }
    }

    private void saveUsernamePeer(TlObject object) {
        switch (object.identifier()) {
            case BaseUser.ID: {
                BaseUser user = (BaseUser) object;
                String username = user.username();
                long accessHash = Objects.requireNonNull(user.accessHash());
                if (username != null) {
                    InputPeer peer = user.self()
                            ? InputPeerSelf.instance()
                            : ImmutableInputPeerUser.of(user.id(), accessHash);

                    // add special tags for indexing
                    if (user.self()) {
                        usernames.putIfAbsent("me", InputPeerSelf.instance());
                        usernames.putIfAbsent("self", InputPeerSelf.instance());
                    }

                    usernames.put(stripUsername(username), peer);
                }
                break;
            }
            case Channel.ID: {
                Channel channel = (Channel) object;

                String username = channel.username();
                long accessHash = Objects.requireNonNull(channel.accessHash());
                if (username != null) {
                    usernames.put(stripUsername(username), ImmutableInputPeerChannel.of(channel.id(), accessHash));
                }
            }
        }
    }

    @Nullable
    private PartialFields<Chat, ChatFull> saveChatMin(Chat chat) {
        if (!isAccessible(chat)) {
            return null;
        }

        Chat cpy = copy(chat);
        var old = chats.computeIfAbsent(cpy.id(), k -> new PartialFields<>(cpy));
        chats.computeIfPresent(cpy.id(), (k, v) -> new PartialFields<>(cpy, v.full));
        saveUsernamePeer(cpy);

        return old;
    }

    private void saveContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        for (Chat chat : chats) {
            saveChatMin(chat);
        }

        for (User user : users) {
            saveUserMin(user);
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends TlObject> T copy(T object) {
        switch (object.identifier()) {
            case BaseChat.ID: return (T) ImmutableBaseChat.copyOf((BaseChat) object);
            case Channel.ID: return (T) ImmutableChannel.copyOf((Channel) object);
            case BaseChatFull.ID: return (T) ImmutableBaseChatFull.copyOf((BaseChatFull) object);
            case ChannelFull.ID: return (T) ImmutableChannelFull.copyOf((ChannelFull) object);
            default: throw new IllegalArgumentException("Unknown entity type: " + object);
        }
    }

    static boolean isAccessible(TlObject obj) {
        switch (obj.identifier()) {
            case ChannelForbidden.ID:
            case ChatForbidden.ID: return false;
            case Channel.ID:
                Channel channel = (Channel) obj;
                return !channel.min() && channel.accessHash() != null;
            case BaseUser.ID:
                BaseUser user = (BaseUser) obj;
                return !user.min() && user.accessHash() != null;
            default:
                return true;
        }
    }

    private Peer asPeer(InputPeer peer) {
        long rawPeerId = getRawInputPeerId(peer);
        switch (peer.identifier()) {
            case InputPeerChat.ID: return ImmutablePeerChat.of(rawPeerId);
            case InputPeerChannel.ID:
            case InputPeerChannelFromMessage.ID: return ImmutablePeerChannel.of(rawPeerId);
            case InputPeerUser.ID:
            case InputPeerUserFromMessage.ID: return ImmutablePeerUser.of(rawPeerId);
            case InputPeerSelf.ID: return ImmutablePeerUser.of(selfId);
            default: throw new IllegalStateException();
        }
    }

    private long getRawInputPeerId(InputPeer peer) {
        switch (peer.identifier()) {
            case InputPeerChat.ID: return ((InputPeerChat) peer).chatId();
            case InputPeerChannel.ID: return ((InputPeerChannel) peer).channelId();
            case InputPeerUser.ID: return ((InputPeerUser) peer).userId();
            case InputPeerChannelFromMessage.ID: return ((InputPeerChannelFromMessage) peer).channelId();
            case InputPeerUserFromMessage.ID: return ((InputPeerUserFromMessage) peer).userId();
            case InputPeerSelf.ID: return selfId;
            default: throw new IllegalStateException();
        }
    }

    private void addContact(Peer peer, Collection<Chat> chats, Collection<User> users) {
        long rawPeerId = getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerChat.ID:
            case PeerChannel.ID:
                var chatInfo = this.chats.get(rawPeerId);
                if (chatInfo != null) {
                    chats.add(chatInfo.min);
                }
                break;
            case PeerUser.ID:
                var userInfo = this.users.get(rawPeerId);
                if (userInfo != null) {
                    users.add(userInfo.min);
                }
                break;
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    static class MessageId {
        final int messageId;
        final long chatId;

        static MessageId create(BaseMessageFields message) {
            switch (message.peerId().identifier()) {
                case PeerChannel.ID:
                    return new MessageId(message.id(), ((PeerChannel) message.peerId()).channelId());
                case PeerChat.ID:
                case PeerUser.ID:
                    return new MessageId(message.id(), -1);
                default: throw new IllegalArgumentException("Unknown peer type: " + message.peerId());
            }
        }

        MessageId(int messageId, long chatId) {
            this.messageId = messageId;
            this.chatId = chatId;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageId messageId1 = (MessageId) o;
            return messageId == messageId1.messageId && chatId == messageId1.chatId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, chatId);
        }
    }

    static class PartialFields<M, F> {
        private final M min;
        @Nullable
        private final F full;

        PartialFields(M min) {
            this(min, null);
        }

        PartialFields(M min, @Nullable F full) {
            this.min = min;
            this.full = full;
        }

        public M getMin() {
            return min;
        }

        @Nullable
        public F getFull() {
            return full;
        }

        public PartialFields<M, F> withMin(M min) {
            if (Objects.equals(this.min, min)) {
                return this;
            }
            return new PartialFields<>(min, full);
        }

        public PartialFields<M, F> withFull(@Nullable F full) {
            if (Objects.equals(this.full, full)) {
                return this;
            }
            return new PartialFields<>(min, full);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PartialFields<?, ?> that = (PartialFields<?, ?>) o;
            return min.equals(that.min) && Objects.equals(full, that.full);
        }

        @Override
        public int hashCode() {
            return Objects.hash(min, full);
        }
    }
}
