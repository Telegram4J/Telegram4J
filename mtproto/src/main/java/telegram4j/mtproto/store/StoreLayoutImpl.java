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

/** Default in-memory store implementation. */
public class StoreLayoutImpl implements StoreLayout {

    private final Cache<MessageId, BaseMessageFields> messages;
    private final ConcurrentMap<Long, PartialFields<BaseChat, BaseChatFull>> chats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PartialFields<Channel, ChannelFull>> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PartialFields<ImmutableBaseUser, ImmutableUserFull>> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Peer> usernames = new ConcurrentHashMap<>();
    private final ConcurrentMap<Peer, InputPeer> peers = new ConcurrentHashMap<>();
    private final ConcurrentMap<DataCenter, AuthorizationKeyHolder> authKeys = new ConcurrentHashMap<>();

    private volatile long selfId = -1;
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
        return Mono.justOrEmpty(selfId).filter(l -> l != -1);
    }

    @Override
    public Mono<ResolvedPeer> resolvePeer(String username) {
        return Mono.fromSupplier(() -> usernames.get(stripUsername(username)))
                .mapNotNull(p -> {
                    List<User> user = new ArrayList<>(1);
                    List<Chat> chat = new ArrayList<>(1);
                    addContact(p, chat, user);

                    return ResolvedPeer.builder()
                            .users(user)
                            .chats(chat)
                            .peer(p)
                            .build();
                });
    }

    @Override
    public Mono<InputUser> resolveUser(long userId) {
        return Mono.fromSupplier(() -> peers.get(ImmutablePeerUser.of(userId)))
                .map(TlEntityUtil::toInputUser);
    }

    @Override
    public Mono<InputChannel> resolveChannel(long channelId) {
        return Mono.fromSupplier(() -> peers.get(ImmutablePeerChannel.of(channelId)))
                .map(TlEntityUtil::toInputChannel);
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
                            case InputMessageReplyTo.ID:
                            case InputMessageCallbackQuery.ID:
                            default: throw new IllegalArgumentException("Unknown message id type: " + id);
                        }
                    })
                    .map(i -> new MessageId(i, -1))
                    .collect(Collectors.toSet());

            var messagesMap = this.messages.getAllPresent(ids);
            if (messagesMap.isEmpty()) {
                return null;
            }

            var messages = messagesMap.values();

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
                            case InputMessagePinned.ID:
                                return Optional.ofNullable(this.channels.get(channelId))
                                        .map(PartialFields::getFull)
                                        .map(ChatFull::pinnedMsgId)
                                        .orElse(null);
                            case InputMessageReplyTo.ID:
                            case InputMessageCallbackQuery.ID:
                            default:
                                throw new IllegalArgumentException("Unknown message id type: " + id);
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(i -> new MessageId(i, channelId))
                    .collect(Collectors.toSet());

            var messagesMap = this.messages.getAllPresent(ids);
            if (messagesMap.isEmpty()) {
                return null;
            }

            var messages = messagesMap.values();

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
    public Mono<telegram4j.tl.messages.ChatFull> getChannelFullById(long channelId) {
        return Mono.fromSupplier(() -> channels.get(channelId))
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
        return Mono.fromSupplier(() -> authKeys.get(dc));
    }

    // Updates methods
    // ==================

    @Override
    public Mono<Void> onNewMessage(Message message) {
        return Mono.fromRunnable(() -> {
            BaseMessageFields cast = (BaseMessageFields) message;
            MessageId key = MessageId.create(cast);

            messages.put(key, cast);

            Peer p = cast.fromId();
            if (p != null) {
                savePeer(p, cast);
            }
        });
    }

    @Override
    public Mono<Message> onEditMessage(Message message) {
        return Mono.fromSupplier(() -> {
            BaseMessageFields cast = (BaseMessageFields) message;
            MessageId key = MessageId.create(cast);

            Peer p = cast.fromId();
            if (p != null) {
                savePeer(p, cast);
            }

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
                    peer = peers.getOrDefault(ImmutablePeerChannel.of(channelId), InputPeerEmpty.instance());
                    break;
                case UpdateDeleteScheduledMessages.ID:
                    Peer p = ((UpdateDeleteScheduledMessages) update).peer();
                    peer = peers.getOrDefault(p, InputPeerEmpty.instance());
                    break;
                case UpdateDeleteMessages.ID:
                    peer = update.messages().stream()
                            .map(i -> new MessageId(i, -1))
                            .map(messages.asMap()::remove)
                            .filter(Objects::nonNull)
                            .map(m -> peers.getOrDefault(m.peerId(), InputPeerEmpty.instance()))
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
    public Mono<Void> onChannelUserTyping(UpdateChannelUserTyping payload) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatUserTyping(UpdateChatUserTyping payload) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onUserTyping(UpdateUserTyping payload) {
        return Mono.empty();
    }

    @Override
    public Mono<UserNameFields> onUserNameUpdate(UpdateUserName payload) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(payload.userId());
            UserNameFields fields = old != null ? new UserNameFields(old.min.username(), old.min.firstName(), old.min.lastName()) : null;

            Optional.ofNullable(fields)
                    .map(UserNameFields::getUserName)
                    .ifPresent(s -> usernames.remove(stripUsername(s)));

            this.users.computeIfPresent(payload.userId(), (k, v) -> {
                var upd = v.min
                        .withUsername(payload.username())
                        .withFirstName(payload.firstName())
                        .withLastName(payload.lastName());

                saveUsernamePeer(upd);
                return v.withMin(upd);
            });

            return fields;
        });
    }

    @Override
    public Mono<String> onUserPhoneUpdate(UpdateUserPhone payload) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(payload.userId());
            this.users.computeIfPresent(payload.userId(), (k, v) -> v.withMin(v.min.withPhone(payload.phone())));

            return old != null ? old.min.phone() : null;
        });
    }

    @Override
    public Mono<UserProfilePhoto> onUserPhotoUpdate(UpdateUserPhoto payload) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(payload.userId());
            this.users.computeIfPresent(payload.userId(), (k, v) -> v.withMin(v.min.withPhoto(payload.photo())));

            return old != null ? old.min.photo() : null;
        });
    }

    @Override
    public Mono<UserStatus> onUserStatusUpdate(UpdateUserStatus payload) {
        return Mono.fromSupplier(() -> {
            var old = this.users.get(payload.userId());
            this.users.computeIfPresent(payload.userId(), (k, v) -> v.withMin(v.min.withStatus(payload.status())));

            return old != null ? old.min.status() : null;
        });
    }

    @Override
    public Mono<Void> onChatParticipantAdd(UpdateChatParticipantAdd payload) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipantAdmin(UpdateChatParticipantAdmin payload) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipantDelete(UpdateChatParticipantDelete payload) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipant(UpdateChatParticipant payload) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChatParticipants(ChatParticipants payload) {
        return Mono.empty();
    }

    @Override
    public Mono<Void> onChannelParticipant(UpdateChannelParticipant payload) {
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
        return Mono.fromRunnable(() -> authKeys.put(authorizationKey.getDc(), authorizationKey));
    }

    @Override
    public Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        return Mono.fromRunnable(() -> saveContacts(chats, users));
    }

    @Override
    public Mono<Void> onUserUpdate(User payload) {
        return Mono.fromRunnable(() -> saveUserMin(payload));
    }

    @Override
    public Mono<Void> onUserUpdate(telegram4j.tl.users.UserFull payload) {
        return Mono.fromRunnable(() -> saveUserFull(payload));
    }

    @Override
    public Mono<Void> onChatUpdate(Chat payload) {
        return Mono.fromRunnable(() -> saveChatMin(payload));
    }

    @Override
    public Mono<Void> onChatUpdate(telegram4j.tl.messages.ChatFull payload) {
        return Mono.fromRunnable(() -> saveChatFull(payload));
    }

    @Override
    public Mono<Void> onResolvedPeer(ResolvedPeer payload) {
        return Mono.fromRunnable(() -> saveContacts(payload.chats(), payload.users()));
    }

    private void saveContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        for (Chat chat : chats) {
            saveChatMin(chat);
        }

        for (User user : users) {
            saveUserMin(user);
        }
    }

    private void saveChatFull(telegram4j.tl.messages.ChatFull chat) {
        ChatFull chat0 = copy(chat.fullChat());
        var chat1 = chat.chats().stream()
                .filter(c -> isAccessible(c) && c.id() == chat0.id())
                .findFirst()
                .map(StoreLayoutImpl::copy)
                .orElse(null);

        if (chat1 == null || !isAccessible(chat1)) {
            return;
        }

        if (chat1.identifier() == BaseChat.ID) {
            chats.put(chat0.id(), new PartialFields<>((BaseChat) chat1, (BaseChatFull) chat0));
        } else { // Channel
            channels.put(chat0.id(), new PartialFields<>((Channel) chat1, (ChannelFull) chat0));
            saveUsernamePeer(chat0);
        }

        Peer p = chat0.identifier() == ChannelFull.ID
                ? ImmutablePeerChannel.of(chat0.id())
                : ImmutablePeerChat.of(chat0.id());

        savePeer(p, null);
    }

    private void saveUserFull(telegram4j.tl.users.UserFull user) {
        var user0 = ImmutableUserFull.copyOf(user.fullUser());
        var user1 = user.users().stream()
                .filter(u -> isAccessible(u) && u.id() == user0.id())
                .map(u -> (BaseUser) u)
                .findFirst()
                .map(ImmutableBaseUser::copyOf)
                .orElse(null);

        if (user1 == null || !isAccessible(user1)) {
            return;
        }

        // Updating self info on first getUserFull for self user
        if (user1.self()) {
            selfId = user0.id();

            // add special tags for indexing
            var self = ImmutablePeerUser.of(selfId);
            usernames.put("me", self);
            usernames.put("self", self);
        }

        users.put(user0.id(), new PartialFields<>(user1, user0));
        savePeer(ImmutablePeerUser.of(user0.id()), null);
        saveUsernamePeer(user1);
    }

    private void saveUserMin(User user) {
        if (user.identifier() != BaseUser.ID || !isAccessible(user)) {
            return;
        }

        ImmutableBaseUser user0 = ImmutableBaseUser.copyOf((BaseUser) user);
        users.compute(user0.id(), (k, v) -> v == null ? new PartialFields<>(user0) : v.withMin(user0));
        savePeer(ImmutablePeerUser.of(user0.id()), null);
        saveUsernamePeer(user0);
    }

    private void saveChatMin(Chat chat) {
        if (!isAccessible(chat)) {
            return;
        }

        Chat cpy = copy(chat);
        if (chat.identifier() == BaseChat.ID) {
            chats.compute(cpy.id(), (k, v) -> v == null ? new PartialFields<>((BaseChat) cpy) : v.withMin((BaseChat) cpy));
        } else { // Channel
            channels.compute(cpy.id(), (k, v) -> v == null ? new PartialFields<>((Channel) cpy) : v.withMin((Channel) cpy));
            saveUsernamePeer(cpy);
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

    private boolean isAccessible(TlObject obj) {
        switch (obj.identifier()) {
            case ChannelForbidden.ID:
            case ChatForbidden.ID: return false;
            case Channel.ID:
                Channel channel = (Channel) obj;
                var channelInfo = channels.get(channel.id());

                return !channel.min() && channelInfo != null && !channelInfo.min.min()
                        && channelInfo.min.accessHash() != null
                        || channel.accessHash() != null;
            case BaseUser.ID:
                BaseUser user = (BaseUser) obj;
                var userInfo = users.get(user.id());

                return !user.min() && userInfo != null && !userInfo.min.min()
                        && userInfo.min.accessHash() != null
                        || user.accessHash() != null;
            default:
                return true;
        }
    }

    private boolean isBot() {
        var userInfo = users.get(selfId);
        Objects.requireNonNull(userInfo, "self user isn't yet retrieved.");
        return userInfo.min.bot();
    }

    private void saveUsernamePeer(TlObject object) {
        switch (object.identifier()) {
            case BaseUser.ID: {
                BaseUser user = (BaseUser) object;
                String username = user.username();
                if (username != null) {
                    usernames.put(stripUsername(username), ImmutablePeerUser.of(user.id()));
                }
                break;
            }
            case Channel.ID: {
                Channel channel = (Channel) object;

                String username = channel.username();
                if (username != null) {
                    usernames.put(stripUsername(username), ImmutablePeerChannel.of(channel.id()));
                }
            }
        }
    }

    private void savePeer(Peer p, @Nullable BaseMessageFields message) {
        if (peers.containsKey(p)) {
            return;
        }

        long peerId = getRawPeerId(p);
        InputPeer in;
        switch (p.identifier()) {
            case PeerChat.ID:
                in = ImmutableInputPeerChat.of(peerId);
                break;
            case PeerChannel.ID:
                var chatInfo = channels.get(peerId);
                Channel ch = chatInfo != null ? chatInfo.min : null;
                if ((ch == null || !isAccessible(ch)) && (isBot() || message == null)) {
                    return;
                }

                if ((ch == null || !isAccessible(ch)) && message != null) {
                    InputPeer chatPeer = peers.get(message.peerId());
                    if (chatPeer == null) {
                        return;
                    }

                    in = ImmutableInputPeerChannelFromMessage.of(chatPeer, message.id(), peerId);
                } else {
                    Long accessHash = ch.accessHash();
                    if (accessHash == null) {
                        return;
                    }
                    in = ImmutableInputPeerChannel.of(peerId, accessHash);
                }

                break;
            case PeerUser.ID:
                if (selfId == peerId) {
                    in = InputPeerSelf.instance();
                    break;
                }

                var userInfo = users.get(peerId);
                BaseUser us = userInfo != null ? userInfo.min : null;

                if ((us == null || !isAccessible(us)) && (isBot() || message == null)) {
                    return;
                }

                if ((us == null || !isAccessible(us)) && message != null) {
                    InputPeer chatPeer = peers.get(message.peerId());
                    if (chatPeer == null) {
                        return;
                    }

                    in = ImmutableInputPeerUserFromMessage.of(chatPeer, message.id(), peerId);
                } else {
                    Long accessHash = us.accessHash();
                    if (accessHash == null) {
                        return;
                    }
                    in = ImmutableInputPeerUser.of(peerId, accessHash);
                }
                break;
            default:
                return;
        }

        peers.put(p, in);
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
                var chatInfo = this.channels.get(rawPeerId);
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
            default:
                throw new IllegalArgumentException("Unknown peer type: " + peer);
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
            this.min = Objects.requireNonNull(min, "min");
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
