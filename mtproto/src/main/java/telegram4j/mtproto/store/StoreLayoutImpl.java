package telegram4j.mtproto.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcOptions;
import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.store.object.*;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.ChatFull;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.channels.BaseChannelParticipants;
import telegram4j.tl.channels.ImmutableChannelParticipant;
import telegram4j.tl.contacts.ImmutableResolvedPeer;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.*;
import telegram4j.tl.updates.ImmutableState;
import telegram4j.tl.updates.State;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static telegram4j.mtproto.util.TlEntityUtil.getUserId;
import static telegram4j.mtproto.util.TlEntityUtil.stripUsername;

/** Default in-memory store implementation. */
public class StoreLayoutImpl implements StoreLayout {

    private final Cache<MessageId, BaseMessageFields> messages;
    private final ConcurrentMap<Long, ChatInfo> chats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ChannelInfo> channels = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PartialFields<ImmutableBaseUser, ImmutableUserFull>> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, MessagePoll> polls = new ConcurrentHashMap<>();
    // TODO: make weak or limit by size?
    private final ConcurrentMap<String, Peer> usernames = new ConcurrentHashMap<>();
    private final ConcurrentMap<Peer, InputPeer> peers = new ConcurrentHashMap<>();
    private final ConcurrentMap<DcKey, AuthorizationKeyHolder> authKeys = new ConcurrentHashMap<>();

    private volatile DataCenter dataCenter;
    private volatile long selfId;
    private volatile ImmutableState state;
    private volatile PublicRsaKeyRegister publicRsaKeyRegister;
    private volatile DcOptions dcOptions;
    private volatile Config config;

    public StoreLayoutImpl(Function<Caffeine<Object, Object>, Caffeine<Object, Object>> cacheFactory) {
        this.messages = cacheFactory.apply(Caffeine.newBuilder()).build();
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.empty();
    }

    @Override
    public Mono<DataCenter> getDataCenter() {
        return Mono.justOrEmpty(dataCenter);
    }

    @Override
    public Mono<State> getCurrentState() {
        return Mono.justOrEmpty(state);
    }

    @Override
    public Mono<Long> getSelfId() {
        return Mono.just(selfId).filter(l -> l != 0);
    }

    @Override
    public Mono<ResolvedPeer> resolvePeer(String username) {
        return Mono.fromSupplier(() -> usernames.get(stripUsername(username)))
                .mapNotNull(p -> {
                    List<User> user = new ArrayList<>(1);
                    List<Chat> chat = new ArrayList<>(1);
                    addContact(p, chat, user);

                    return ImmutableResolvedPeer.of(p, chat, user);
                });
    }

    @Override
    public Mono<InputPeer> resolvePeer(Peer peerId) {
        return Mono.fromSupplier(() -> peers.get(peerId));
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
                    .map(id -> {
                        switch (id.identifier()) {
                            case InputMessageID.ID: return ((InputMessageID) id).id();
                            case InputMessagePinned.ID:
                            case InputMessageReplyTo.ID:
                            case InputMessageCallbackQuery.ID:
                                throw new UnsupportedOperationException("Message id type: " + id);
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

            return ImmutableBaseMessages.of(messages, chats, users);
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
                                        .map(c -> c.full)
                                        .map(ChatFull::pinnedMsgId)
                                        .orElse(null);
                            case InputMessageReplyTo.ID:
                            case InputMessageCallbackQuery.ID:
                                throw new UnsupportedOperationException("Message id type: " + id);
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

            return ImmutableBaseMessages.of(messages, chats, users);
        });
    }

    @Override
    public Mono<BaseChat> getChatMinById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId)).map(c -> c.min);
    }

    @Override
    public Mono<telegram4j.tl.messages.ChatFull> getChatFullById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId))
                .mapNotNull(chatInfo -> {
                    if (chatInfo.full == null) {
                        return null;
                    }

                    List<BaseUser> users = new ArrayList<>();
                    if (chatInfo.participants != null) {
                        for (var p : chatInfo.participants.values()) {
                            var u = this.users.get(p.userId());
                            if (u != null) {
                                users.add(u.min);
                            }
                        }
                    }
                    var botInfo = chatInfo.full.botInfo();
                    if (botInfo != null) {
                        for (BotInfo info : botInfo) {
                            var u = this.users.get(Objects.requireNonNull(info.userId()));
                            if (u != null) {
                                users.add(u.min);
                            }
                        }
                    }

                    return telegram4j.tl.messages.ChatFull.builder()
                            .users(users)
                            .chats(List.of(chatInfo.min))
                            .fullChat(chatInfo.full)
                            .build();
                });
    }

    @Override
    public Mono<ChatData<BaseChat, BaseChatFull>> getChatById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId))
                .map(chatInfo -> {
                    List<BaseUser> users = new ArrayList<>();
                    if (chatInfo.participants != null) {
                        for (var p : chatInfo.participants.values()) {
                            var u = this.users.get(p.userId());
                            if (u != null) {
                                users.add(u.min);
                            }
                        }
                    }
                    List<BotInfo> botInfo;
                    if (chatInfo.full != null && (botInfo = chatInfo.full.botInfo()) != null) {
                        for (BotInfo info : botInfo) {
                            var u = this.users.get(Objects.requireNonNull(info.userId()));
                            if (u != null) {
                                users.add(u.min);
                            }
                        }
                    }

                    return new ChatData<>(chatInfo.min, chatInfo.full, users);
                });
    }

    @Override
    public Mono<Channel> getChannelMinById(long channelId) {
        return Mono.fromSupplier(() -> channels.get(channelId)).map(c -> c.min);
    }

    @Override
    public Mono<telegram4j.tl.messages.ChatFull> getChannelFullById(long channelId) {
        return Mono.fromSupplier(() -> channels.get(channelId))
                .filter(channelInfo -> channelInfo.full != null)
                .map(channelInfo -> {
                    List<BaseUser> users = new ArrayList<>();
                    if (channelInfo.full != null) {
                        for (BotInfo info : channelInfo.full.botInfo()) {
                            var u = this.users.get(Objects.requireNonNull(info.userId()));
                            if (u != null) {
                                users.add(u.min);
                            }
                        }
                    }

                    return telegram4j.tl.messages.ChatFull.builder()
                            .chats(List.of(channelInfo.min))
                            .users(users)
                            .fullChat(Objects.requireNonNull(channelInfo.full))
                            .build();
                });
    }

    @Override
    public Mono<ChatData<Channel, ChannelFull>> getChannelById(long channelId) {
        return Mono.fromSupplier(() -> channels.get(channelId))
                .map(channelInfo -> {
                    List<BaseUser> users = new ArrayList<>();
                    if (channelInfo.full != null) {
                        for (BotInfo info : channelInfo.full.botInfo()) {
                            var u = this.users.get(Objects.requireNonNull(info.userId()));
                            if (u != null) {
                                users.add(u.min);
                            }
                        }
                    }

                    return new ChatData<>(channelInfo.min, channelInfo.full, users);
                });
    }

    @Override
    public Mono<BaseUser> getUserMinById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId)).map(c -> c.min);
    }

    @Override
    public Mono<telegram4j.tl.users.UserFull> getUserFullById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId))
                .filter(userInfo -> userInfo.full != null)
                .map(userInfo -> telegram4j.tl.users.UserFull.builder()
                        .users(List.of(userInfo.min))
                        .fullUser(Objects.requireNonNull(userInfo.full))
                        .chats(List.of())
                        .build());
    }

    @Override
    public Mono<PeerData<BaseUser, UserFull>> getUserById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId))
                .map(userInfo -> new PeerData<>(userInfo.min, userInfo.full));
    }

    @Override
    public Mono<telegram4j.tl.channels.ChannelParticipant> getChannelParticipantById(long channelId, Peer peerId) {
        return Mono.fromSupplier(() -> channels.get(channelId))
                .mapNotNull(c -> c.participants)
                .mapNotNull(c -> c.get(peerId))
                .map(p -> {
                    List<User> user = new ArrayList<>(1);
                    List<Chat> chat = new ArrayList<>(1);
                    addContact(peerId, chat, user);
                    return ImmutableChannelParticipant.of(p, chat, user);
                });
    }

    @Override
    public Mono<ResolvedChatParticipant> getChatParticipantById(long chatId, long userId) {
        return Mono.fromSupplier(() -> chats.get(chatId))
                .mapNotNull(c -> c.participants)
                .mapNotNull(c -> c.get(userId))
                .map(c -> {
                    var user = users.get(userId);
                    return new ResolvedChatParticipant(c, user != null ? user.min : null);
                });
    }

    @Override
    public Flux<telegram4j.tl.channels.ChannelParticipant> getChannelParticipants(long channelId) {
        return Mono.fromSupplier(() -> channels.get(channelId))
                .mapNotNull(c -> c.participants)
                .flatMapIterable(Map::entrySet)
                .map(e -> {
                    List<User> user = new ArrayList<>(1);
                    List<Chat> chat = new ArrayList<>(1);
                    addContact(e.getKey(), chat, user);
                    return ImmutableChannelParticipant.of(e.getValue(), chat, user);
                });
    }

    @Override
    public Flux<ResolvedChatParticipant> getChatParticipants(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId))
                .mapNotNull(c -> c.participants)
                .flatMapIterable(Map::values)
                .map(e -> {
                    var user = users.get(e.userId());
                    return new ResolvedChatParticipant(e, user != null ? user.min : null);
                });
    }

    @Override
    public Mono<MessagePoll> getPollById(long pollId) {
        return Mono.fromSupplier(() -> polls.get(pollId));
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthKey(DataCenter dc) {
        return Mono.fromSupplier(() -> authKeys.get(DcKey.create(dc)));
    }

    @Override
    public Mono<Config> getConfig() {
        return Mono.justOrEmpty(config);
    }

    @Override
    public Mono<DcOptions> getDcOptions() {
        return Mono.justOrEmpty(dcOptions);
    }

    @Override
    public Mono<PublicRsaKeyRegister> getPublicRsaKeyRegister() {
        return Mono.justOrEmpty(publicRsaKeyRegister);
    }

    // Updates methods
    // ==================

    @Override
    public Mono<Void> onNewMessage(Message update) {
        return Mono.fromRunnable(() -> saveMessage(update));
    }

    @Override
    public Mono<Message> onEditMessage(Message update) {
        return Mono.fromSupplier(() -> {
            BaseMessageFields cast = copy((BaseMessageFields) update);
            MessageId key = MessageId.create(cast);

            savePeer(cast.peerId(), cast);
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
                            .map(messages.asMap()::get)
                            .filter(Objects::nonNull)
                            .map(m -> peers.getOrDefault(m.peerId(), InputPeerEmpty.instance()))
                            .findFirst()
                            .orElse(InputPeerEmpty.instance());
                    break;
                default:
                    throw new IllegalStateException("Unexpected update type: " + update);
            }

            long rawPeerId;
            switch (peer.identifier()) {
                case InputPeerEmpty.ID:
                    return null;
                case InputPeerChannel.ID:
                    var channel = (InputPeerChannel) peer;
                    rawPeerId = channel.channelId();
                    break;
                case InputPeerChannelFromMessage.ID:
                    var minChannel = (InputPeerChannelFromMessage) peer;
                    rawPeerId = minChannel.channelId();
                    break;
                case InputPeerChat.ID:
                case InputPeerSelf.ID:
                case InputPeerUser.ID:
                case InputPeerUserFromMessage.ID:
                    rawPeerId = -1;
                    break;
                default:
                    throw new IllegalStateException("Unknown peer type: " + peer);
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
    public Mono<Void> onChatParticipant(UpdateChatParticipant payload) {
        return Mono.fromRunnable(() -> chats.computeIfPresent(payload.chatId(), (k, v) -> {
            var curr = payload.newParticipant();
            var map = v.participants;
            if (curr == null) {
                if (map != null) {
                    map.remove(payload.userId());
                }
            } else {
                map = new ConcurrentHashMap<>();
                map.put(curr.userId(), copy(curr));
            }

            return v.withParticipants(map);
        }));
    }

    @Override
    public Mono<Void> onChatParticipants(ChatParticipants payload) {
        return Mono.fromRunnable(() -> chats.computeIfPresent(payload.chatId(), (k, v) -> {
            // ignore this updates received for unknown channels for preventing inconsistency
            if (v.full == null) {
                return v;
            }

            switch (payload.identifier()) {
                case BaseChatParticipants.ID: {
                    BaseChatParticipants base = (BaseChatParticipants) payload;

                    BaseChatParticipants old = v.full.participants() instanceof BaseChatParticipants
                            ? (BaseChatParticipants) v.full.participants()
                            : null;

                    // no update; just ignore received update
                    // This check may create inconsistency
                    if (old != null && base.version() < old.version()) {
                        return v;
                    }

                    var map = v.participants();
                    map.clear();
                    for (var p : base.participants()) {
                        var copy = copy(p);
                        map.put(copy.userId(), copy);
                    }

                    return v.withFull(f -> f.withParticipants(payload))
                            .withParticipants(map);
                }
                case ChatParticipantsForbidden.ID:
                    ChatParticipantsForbidden forbidden = (ChatParticipantsForbidden) payload;
                    var updated = v.withFull(f -> f.withParticipants(payload));
                    var self = forbidden.selfParticipant();
                    if (self != null) {
                        var map = updated.participants();
                        var copy = copy(self);
                        map.put(copy.userId(), copy(self));
                        updated = updated.withParticipants(map);
                    }

                    return updated;
                default:
                    throw new IllegalArgumentException("Unexpected ChatParticipants type: " + payload);
            }
        }));
    }

    @Override
    public Mono<Void> onChannelParticipant(UpdateChannelParticipant payload) {
        return Mono.fromRunnable(() -> channels.computeIfPresent(payload.channelId(), (k, v) -> {
            var prev = payload.prevParticipant();
            var curr = payload.prevParticipant();
            var map = v.participants;
            if (curr == null) {
                if (map != null) {
                    Objects.requireNonNull(prev); // TODO: test it
                    map.remove(getUserId(prev));
                }
            } else {
                map = new ConcurrentHashMap<>();
                map.put(getUserId(curr), copy(curr));
            }

            return v;
        }));
    }

    @Override
    public Mono<Void> updateDataCenter(DataCenter dc) {
        Objects.requireNonNull(dc);
        return Mono.fromRunnable(() -> dataCenter = dc);
    }

    @Override
    public Mono<Void> updateState(State state) {
        Objects.requireNonNull(state);
        return Mono.fromRunnable(() -> this.state = ImmutableState.copyOf(state));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(DataCenter dc, AuthorizationKeyHolder authKey) {
        Objects.requireNonNull(dc);
        Objects.requireNonNull(authKey);
        return Mono.fromRunnable(() -> authKeys.put(DcKey.create(dc), authKey));
    }

    @Override
    public Mono<Void> updateChannelPts(long channelId, int pts) {
        return Mono.fromRunnable(() -> channels.computeIfPresent(channelId, (k, v) -> v.withFull(m -> m.withPts(pts))));
    }

    @Override
    public Mono<Void> registerPoll(Peer peerId, int messageId, InputMediaPoll poll) {
        return Mono.fromRunnable(() -> polls.put(poll.poll().id(), new MessagePoll(poll, peerId, messageId)));
    }

    @Override
    public Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        return Mono.fromRunnable(() -> saveContacts(chats, users));
    }

    @Override
    public Mono<Void> onUserUpdate(telegram4j.tl.users.UserFull payload) {
        return Mono.fromRunnable(() -> {
            var userFull = ImmutableUserFull.copyOf(payload.fullUser());
            var anyUser = payload.users().stream()
                    .filter(u -> u.id() == userFull.id())
                    .findFirst()
                    .orElseThrow();

            saveUser(userFull, anyUser);
        });
    }

    @Override
    public Mono<Void> onChatUpdate(telegram4j.tl.messages.ChatFull payload) {
        return Mono.fromRunnable(() -> {
            var anyChat = payload.chats().stream()
                    .filter(c -> c.id() == payload.fullChat().id())
                    .findFirst()
                    .orElseThrow();
            saveContacts(List.of(), payload.users());
            saveChat(payload.fullChat(), anyChat);
        });
    }

    @Override
    public Mono<Void> onChannelParticipants(long channelId, BaseChannelParticipants payload) {
        return Mono.fromRunnable(() -> {
            saveContacts(payload.chats(), payload.users());

            channels.computeIfPresent(channelId, (k, v) -> {
                var map = v.participants();
                for (var p : payload.participants()) {
                    var copy = copy(p);
                    map.put(TlEntityUtil.getUserId(copy), copy);
                }
                return v.withParticipants(map);
            });
        });
    }

    @Override
    public Mono<Void> onChannelParticipant(long channelId, telegram4j.tl.channels.ChannelParticipant payload) {
        return Mono.fromRunnable(() -> {
            saveContacts(payload.chats(), payload.users());

            channels.computeIfPresent(channelId, (k, v) -> {
                var copy = copy(payload.participant());
                var map = v.participants();
                map.put(TlEntityUtil.getUserId(copy), copy);
                return v.withParticipants(map);
            });
        });
    }

    @Override
    public Mono<Void> onMessages(Messages payload) {
        return Mono.fromRunnable(() -> {
            switch (payload.identifier()) {
                case BaseMessages.ID:
                    BaseMessages base = (BaseMessages) payload;

                    saveContacts(base.chats(), base.users());
                    for (var msg : base.messages()) {
                        saveMessage(msg);
                    }
                    break;
                case ChannelMessages.ID:
                    ChannelMessages channel = (ChannelMessages) payload;

                    saveContacts(channel.chats(), channel.users());
                    for (var msg : channel.messages()) {
                        saveMessage(msg);
                    }
                    break;
                case MessagesSlice.ID:
                    MessagesSlice slice = (MessagesSlice) payload;

                    saveContacts(slice.chats(), slice.users());
                    for (var msg : slice.messages()) {
                        saveMessage(msg);
                    }
                    break;
            }
        });
    }

    @Override
    public Mono<Void> onAuthorization(BaseAuthorization auth) {
        return Mono.fromRunnable(() -> saveUser(null, auth.user()));
    }

    @Override
    public Mono<Void> updateDcOptions(DcOptions dcOptions) {
        return Mono.fromRunnable(() -> this.dcOptions = dcOptions);
    }

    @Override
    public Mono<Void> onUpdateConfig(Config config) {
        return Mono.fromRunnable(() -> {
            this.config = config;
            this.dcOptions = DcOptions.from(config);
        });
    }

    @Override
    public Mono<Void> updatePublicRsaKeyRegister(PublicRsaKeyRegister publicRsaKeyRegister) {
        return Mono.fromRunnable(() -> this.publicRsaKeyRegister = publicRsaKeyRegister);
    }

    private void saveContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        for (Chat chat : chats) {
            saveChat(null, chat);
        }

        for (User user : users) {
            saveUser(null, user);
        }
    }

    private void saveUser(@Nullable ImmutableUserFull anyUserFull, User anyUser) {
        if (!(anyUser instanceof BaseUser)) {
            return;
        }

        BaseUser user = (BaseUser) anyUser;
        var userInfo = users.get(user.id());

        saveUsernamePeer(user);

        // received user is min, and we have non-min user, just ignore received.
        if (user.min() && userInfo != null && !userInfo.min.min() && userInfo.min.accessHash() != null) {
            return;
        }

        var userCopy = ImmutableBaseUser.copyOf(user);
        users.compute(userCopy.id(), (k, v) -> {
            var userFull = Optional.ofNullable(anyUserFull)
                    .map(ImmutableUserFull::copyOf)
                    .or(() -> Optional.ofNullable(v).map(u -> u.full))
                    .orElse(null);
            return new PartialFields<>(userCopy, userFull);
        });
        Long acch = userCopy.accessHash();
        if (userCopy.self()) {
            selfId = userCopy.id();

            var self = ImmutablePeerUser.of(userCopy.id());

            // add special tags for indexing
            usernames.putIfAbsent("me", self);
            usernames.putIfAbsent("self", self);

            peers.put(self, InputPeerSelf.instance());

        // BaseUser#accessHash() or Channel#accessHash() can be _min_ hash which
        // allows to download profile photos, but can't be used as parameter
        // of getFullChat or getChannels
        // https://core.telegram.org/api/min
        } else if (acch != null && !userCopy.min()) {
            peers.put(ImmutablePeerUser.of(userCopy.id()), ImmutableInputPeerUser.of(userCopy.id(), acch));
        }
        // if user is min and received from message update,
        // then the *FromMessage peer would be saved in savePeer()
    }

    private void saveChat(@Nullable ChatFull anyChatFull, Chat anyChat) {
        switch (anyChat.identifier()) {
            case ChannelForbidden.ID:
                ChannelForbidden channelForbidden = (ChannelForbidden) anyChat;
                var inputPeer = ImmutableInputPeerChannel.of(channelForbidden.id(), channelForbidden.accessHash());
                peers.put(ImmutablePeerChannel.of(channelForbidden.id()), inputPeer);
                channels.remove(channelForbidden.id());
                return;
            case ChatForbidden.ID:
                peers.putIfAbsent(ImmutablePeerChat.of(anyChat.id()), ImmutableInputPeerChat.of(anyChat.id()));
                chats.remove(anyChat.id());
                return;
            case BaseChat.ID:
                var chat = ImmutableBaseChat.copyOf((BaseChat) anyChat);

                peers.putIfAbsent(ImmutablePeerChat.of(chat.id()), ImmutableInputPeerChat.of(chat.id()));
                chats.compute(chat.id(), (k, v) -> {
                    var chatFull = Optional.ofNullable(anyChatFull)
                            .map(c -> ImmutableBaseChatFull.copyOf((BaseChatFull) c))
                            .or(() -> Optional.ofNullable(v).map(c -> c.full))
                            .orElse(null);

                    ChatInfo updated = v == null
                            ? new ChatInfo(chat, chatFull)
                            : v.withData(chat, chatFull);

                    if (anyChatFull != null) { // was updated
                        if (chatFull.participants() instanceof BaseChatParticipants) {
                            var base = (BaseChatParticipants) chatFull.participants();

                            var map = updated.participants();
                            map.clear();
                            for (var p : base.participants()) {
                                var copy = copy(p);
                                map.put(copy.userId(), copy);
                            }

                            updated = updated.withParticipants(map);
                        } else {
                            ChatParticipant selfParticipant;
                            if (chatFull.participants() instanceof ChatParticipantsForbidden &&
                                    (selfParticipant = ((ChatParticipantsForbidden) chatFull.participants()).selfParticipant()) != null) {
                                var copy = copy(selfParticipant);
                                var map = updated.participants();
                                map.put(copy.userId(), copy);
                                updated = updated.withParticipants(map);
                            } else {
                                updated = updated.withParticipants(null);
                            }
                        }
                    }

                    return updated;
                });
                return;
            case Channel.ID:
                var channel = (Channel) anyChat;
                var channelInfo = channels.get(channel.id());

                saveUsernamePeer(channel);

                // received channel is min, and we have non-min channel, just ignore received.
                if (channel.min() && channelInfo != null && !channelInfo.min.min() && channelInfo.min.accessHash() != null) {
                    return;
                }

                var channelCopy = ImmutableChannel.copyOf(channel);
                channels.compute(channelCopy.id(), (k, v) -> {
                    var channelFull = Optional.ofNullable(anyChatFull)
                            .map(c -> ImmutableChannelFull.copyOf((ChannelFull) c))
                            .or(() -> Optional.ofNullable(v).map(c -> c.full))
                            .orElse(null);

                    return v == null
                            ? new ChannelInfo(channelCopy, channelFull)
                            : v.withData(channelCopy, channelFull);
                });
                Long acch = channelCopy.accessHash();
                if (acch != null && !channelCopy.min()) { // see saveUser()
                    peers.put(ImmutablePeerChannel.of(channelCopy.id()), ImmutableInputPeerChannel.of(channelCopy.id(), acch));
                }
                // if channel is min and received from message update,
                // then the *FromMessage peer would be saved in savePeer()
        }
    }

    @SuppressWarnings("unchecked")
    static <T extends TlObject> T copy(T object) {
        switch (object.identifier()) {
            case BaseChat.ID: return (T) ImmutableBaseChat.copyOf((BaseChat) object);
            case Channel.ID: return (T) ImmutableChannel.copyOf((Channel) object);
            case BaseChatFull.ID: return (T) ImmutableBaseChatFull.copyOf((BaseChatFull) object);
            case ChannelFull.ID: return (T) ImmutableChannelFull.copyOf((ChannelFull) object);

            case BaseMessage.ID: return (T) ImmutableBaseMessage.copyOf((BaseMessage) object);
            case MessageService.ID: return (T) ImmutableMessageService.copyOf((MessageService) object);

            case BaseChatParticipant.ID: return (T) ImmutableBaseChatParticipant.copyOf((BaseChatParticipant) object);
            case ChatParticipantAdmin.ID: return (T) ImmutableChatParticipantAdmin.copyOf((ChatParticipantAdmin) object);
            case ChatParticipantCreator.ID: return (T) ImmutableChatParticipantCreator.copyOf((ChatParticipantCreator) object);

            case BaseChannelParticipant.ID: return (T) ImmutableBaseChannelParticipant.copyOf((BaseChannelParticipant) object);
            case ChannelParticipantAdmin.ID: return (T) ImmutableChannelParticipantAdmin.copyOf((ChannelParticipantAdmin) object);
            case ChannelParticipantBanned.ID: return (T) ImmutableChannelParticipantBanned.copyOf((ChannelParticipantBanned) object);
            case ChannelParticipantCreator.ID: return (T) ImmutableChannelParticipantCreator.copyOf((ChannelParticipantCreator) object);
            case ChannelParticipantLeft.ID: return (T) ImmutableChannelParticipantLeft.copyOf((ChannelParticipantLeft) object);
            case ChannelParticipantSelf.ID: return (T) ImmutableChannelParticipantSelf.copyOf((ChannelParticipantSelf) object);
            default: throw new IllegalArgumentException("Unknown entity type: " + object);
        }
    }

    private boolean isBot() {
        var userInfo = users.get(selfId());
        Objects.requireNonNull(userInfo);
        return userInfo.min.bot();
    }

    private long selfId() {
        long id = selfId;
        if (id == 0) {
            throw new IllegalStateException("No information about current user.");
        }
        return id;
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
                break;
            }
            default: throw new IllegalStateException("Unexpected peer type: " + object);
        }
    }

    private void savePeer(Peer p, BaseMessageFields message) {
        switch (p.identifier()) {
            case PeerChat.ID:
                var cp = ImmutablePeerChat.copyOf((PeerChat) p);
                peers.putIfAbsent(cp, ImmutableInputPeerChat.of(cp.chatId()));
                break;
            // Here only handling for min objects
            case PeerChannel.ID: {
                var chp = ImmutablePeerChannel.copyOf((PeerChannel) p);
                var channelInfo = channels.get(chp.channelId());

                if ((channelInfo == null || channelInfo.min.min()) && !isBot()) {
                    var chatPeer = peers.get(message.peerId());
                    if (chatPeer == null) {
                        break;
                    }

                    var minChannel = ImmutableInputPeerChannelFromMessage.of(
                            chatPeer, message.id(), chp.channelId());
                    peers.put(chp, minChannel);
                }
                break;
            }
            case PeerUser.ID:
                var up = ImmutablePeerUser.copyOf((PeerUser) p);
                var userInfo = users.get(up.userId());
                if ((userInfo == null || userInfo.min.min()) && !isBot()) {
                    InputPeer chatPeer = peers.get(message.peerId());
                    if (chatPeer == null) {
                        break;
                    }

                    var minUser = ImmutableInputPeerUserFromMessage.of(
                            chatPeer, message.id(), up.userId());
                    peers.put(up, minUser);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected Peer type: " + p);
        }
    }

    private void addContact(Peer p, Consumer<Chat> chats, Consumer<User> users) {
        switch (p.identifier()) {
            case PeerChat.ID:
                var cp = (PeerChat) p;
                var chatInfo = this.chats.get(cp.chatId());
                if (chatInfo != null) {
                    chats.accept(chatInfo.min);
                }
                break;
            case PeerChannel.ID:
                var chp = (PeerChannel) p;
                var channelInfo = this.channels.get(chp.channelId());
                if (channelInfo != null) {
                    chats.accept(channelInfo.min);
                }
                break;
            case PeerUser.ID:
                var up = (PeerUser) p;
                var userInfo = this.users.get(up.userId());
                if (userInfo != null) {
                    users.accept(userInfo.min);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown Peer type: " + p);
        }
    }

    private void addContact(Peer peer, Collection<Chat> chats, Collection<User> users) {
        addContact(peer, chats::add, users::add);
    }

    private void saveMessage(Message message) {
        if (!(message instanceof BaseMessageFields)) return;

        BaseMessageFields copy = copy((BaseMessageFields) message);
        MessageId key = MessageId.create(copy);

        messages.put(key, copy);

        // TODO: extract all possible peers from message?
        savePeer(copy.peerId(), copy);
        Peer p = copy.fromId();
        if (p != null) {
            savePeer(p, copy);
        }

        if (copy instanceof BaseMessage) {
            var base = (BaseMessage) copy;
            var media = base.media();
            if (media != null) {
                switch (media.identifier()) {
                    case MessageMediaPoll.ID:
                        var mmp = (MessageMediaPoll) media;
                        polls.put(mmp.poll().id(), new MessagePoll(mmp.poll(), copy.peerId(), copy.id()));
                        break;
                }
            }
        }
    }

    static class ChannelInfo {
        final ImmutableChannel min;
        @Nullable
        final ImmutableChannelFull full;
        @Nullable // initializes on demand
        final ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants;

        ChannelInfo(ImmutableChannel min, @Nullable ImmutableChannelFull full) {
            this(min, full, null);
        }

        ChannelInfo(ImmutableChannel min, @Nullable ImmutableChannelFull full,
                    @Nullable ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants) {
            this.min = Objects.requireNonNull(min);
            this.full = full;
            this.participants = participants;
        }

        ChannelInfo withParticipants(@Nullable ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants) {
            if (this.participants == participants) return this;
            return new ChannelInfo(min, full, participants);
        }

        ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants() {
            return participants != null ? participants : new ConcurrentHashMap<>();
        }

        ChannelInfo withFull(UnaryOperator<ImmutableChannelFull> mapper) {
            if (this.full == null) return this;
            var full = mapper.apply(this.full);
            if (this.full == full) return this;
            return new ChannelInfo(min, full, participants);
        }

        ChannelInfo withData(ImmutableChannel min, @Nullable ImmutableChannelFull full) {
            if (this.min == min && this.full == full) return this;
            return new ChannelInfo(min, full, participants);
        }

        @Override
        public String toString() {
            return "ChannelInfo{" +
                    "min=" + min +
                    ", full=" + full +
                    ", participants=" + participants +
                    '}';
        }
    }

    static class ChatInfo {
        final ImmutableBaseChat min;
        @Nullable
        final ImmutableBaseChatFull full;
        @Nullable // initializes on demand
        final ConcurrentMap<Long, ChatParticipant> participants;

        ChatInfo(ImmutableBaseChat min, @Nullable ImmutableBaseChatFull full) {
            this(min, full, null);
        }

        ChatInfo(ImmutableBaseChat min, @Nullable ImmutableBaseChatFull full,
                 @Nullable ConcurrentMap<Long, ChatParticipant> participants) {
            this.min = Objects.requireNonNull(min);
            this.full = full;
            this.participants = participants;
        }

        ChatInfo withFull(UnaryOperator<ImmutableBaseChatFull> mapper) {
            if (this.full == null) return this;
            var full = mapper.apply(this.full);
            if (this.full == full) return this;
            return new ChatInfo(min, full, participants);
        }

        ChatInfo withParticipants(@Nullable ConcurrentMap<Long, ChatParticipant> participants) {
            if (this.participants == participants) return this;
            return new ChatInfo(min, full, participants);
        }

        ConcurrentMap<Long, ChatParticipant> participants() {
            return participants != null ? participants : new ConcurrentHashMap<>();
        }

        public ChatInfo withData(ImmutableBaseChat min, @Nullable ImmutableBaseChatFull full) {
            if (this.min == min && this.full == full) return this;
            return new ChatInfo(min, full, participants);
        }
    }

    static class MessageId {
        final int messageId;
        final long chatId; // -1 for DM/Group Chats

        static MessageId create(BaseMessageFields message) {
            switch (message.peerId().identifier()) {
                case PeerChannel.ID:
                    return new MessageId(message.id(), ((PeerChannel) message.peerId()).channelId());
                case PeerChat.ID:
                case PeerUser.ID:
                    return new MessageId(message.id(), -1);
                default: throw new IllegalArgumentException("Unknown Peer type: " + message.peerId());
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
            return messageId ^ Long.hashCode(chatId);
        }
    }

    static class PartialFields<M, F> {
        final M min;
        @Nullable
        final F full;

        PartialFields(M min, @Nullable F full) {
            this.min = Objects.requireNonNull(min);
            this.full = full;
        }
    }

    static class DcKey {
        final int id;
        final DataCenter.Type type;
        final boolean test;

        DcKey(int id, DataCenter.Type type, boolean test) {
            this.id = id;
            this.type = type;
            this.test = test;
        }

        static DcKey create(DataCenter dc) {
            return new DcKey(dc.getId(), dc.getType(), dc.isTest());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DcKey dcKey = (DcKey) o;
            return id == dcKey.id && test == dcKey.test && type == dcKey.type;
        }

        @Override
        public int hashCode() {
            int h = 5381;
            h += (h << 5) + id;
            h += (h << 5) + type.hashCode();
            h += (h << 5) + Boolean.hashCode(test);
            return h;
        }
    }
}
