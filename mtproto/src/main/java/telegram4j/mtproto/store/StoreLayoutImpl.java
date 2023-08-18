/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcOptions;
import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.auth.AuthKey;
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
import java.util.stream.Stream;

import static telegram4j.mtproto.util.TlEntityUtil.getUserId;
import static telegram4j.mtproto.util.TlEntityUtil.stripUsername;

/** Default in-memory store implementation. */
public class StoreLayoutImpl implements StoreLayout {

    protected final Cache<MessageId, Message> messages;
    protected final ConcurrentMap<Long, ChatInfo> chats = new ConcurrentHashMap<>();
    protected final ConcurrentMap<Long, ChannelInfo> channels = new ConcurrentHashMap<>();
    protected final ConcurrentMap<Long, PartialFields<ImmutableBaseUser, ImmutableUserFull>> users = new ConcurrentHashMap<>();
    protected final ConcurrentMap<Long, MessagePoll> polls = new ConcurrentHashMap<>();
    // TODO: make weak or limit by size?
    protected final ConcurrentMap<String, Peer> usernames = new ConcurrentHashMap<>();
    protected final ConcurrentMap<Peer, InputPeer> peers = new ConcurrentHashMap<>();
    protected final ConcurrentMap<DcKey, AuthKey> authKeys = new ConcurrentHashMap<>();

    protected volatile DataCenter dataCenter;
    protected volatile long selfId;
    protected volatile ImmutableState state;
    protected volatile PublicRsaKeyRegister publicRsaKeyRegister;
    protected volatile DcOptions dcOptions;
    protected volatile Config config;

    public StoreLayoutImpl(Function<Caffeine<Object, Object>, Caffeine<Object, Object>> cacheFactory) {
        this.messages = cacheFactory.apply(Caffeine.newBuilder()).build();
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.empty();
    }

    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }

    @Override
    public Mono<DataCenter> getDataCenter() {
        return Mono.fromSupplier(() -> dataCenter);
    }

    @Override
    public Mono<State> getCurrentState() {
        return Mono.fromSupplier(() -> state);
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
    public Mono<Boolean> existMessage(Peer peerId, int messageId) {
        return Mono.fromSupplier(() -> messages.getIfPresent(MessageId.create(peerId, messageId)) != null);
    }

    @Nullable
    private Messages getMessages0(long rawPeerId, Iterable<? extends InputMessage> messageIds) {
        Set<MessageId> ids = new HashSet<>();
        for (InputMessage id : messageIds) {
            int msgId = switch (id.identifier()) {
                case InputMessagePinned.ID -> {
                    if (rawPeerId == -1) {
                        throw new UnsupportedOperationException("Message id type: " + id);
                    }

                    yield Optional.ofNullable(this.channels.get(rawPeerId))
                            .map(c -> c.full)
                            .map(ChatFull::pinnedMsgId)
                            .orElse(null);
                }
                case InputMessageID.ID -> ((InputMessageID) id).id();
                case InputMessageReplyTo.ID, InputMessageCallbackQuery.ID ->
                        throw new UnsupportedOperationException("Message id type: " + id);
                default -> throw new IllegalArgumentException("Unknown message id type: " + id);
            };
            ids.add(new MessageId(rawPeerId, msgId));
        }

        var messagesMap = this.messages.getAllPresent(ids);
        if (messagesMap.isEmpty()) {
            return null;
        }

        var messages = messagesMap.values();

        Set<User> users = new HashSet<>();
        Set<Chat> chats = new HashSet<>();
        for (var message : messages) {
            Peer peerId;
            Peer fromId;
            if (message instanceof BaseMessage b) {
                peerId = b.peerId();
                fromId = b.fromId();
            } else if (message instanceof MessageService s) {
                peerId = s.peerId();
                fromId = s.fromId();
            } else {
                throw new IllegalStateException();
            }

            addContact(peerId, chats, users);
            if (fromId != null) {
                addContact(fromId, chats, users);
            }
        }

        return ImmutableBaseMessages.of(messages, chats, users);
    }

    @Override
    public Mono<Messages> getMessages(Iterable<? extends InputMessage> messageIds) {
        return Mono.fromSupplier(() -> getMessages0(-1, messageIds));
    }

    @Override
    public Mono<Messages> getMessages(long channelId, Iterable<? extends InputMessage> messageIds) {
        return Mono.fromSupplier(() -> getMessages0(channelId, messageIds));
    }

    @Override
    public Mono<Chat> getChatMinById(long chatId) {
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
    public Mono<ChatData<Chat, BaseChatFull>> getChatById(long chatId) {
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
    public Mono<Chat> getChannelMinById(long channelId) {
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
    public Mono<ChatData<Chat, ChannelFull>> getChannelById(long channelId) {
        return Mono.fromSupplier(() -> channels.get(channelId))
                .map(channelInfo -> {
                    var users = new ArrayList<BaseUser>();
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
    public Mono<AuthKey> getAuthKey(DataCenter dc) {
        return Mono.fromSupplier(() -> authKeys.get(DcKey.create(dc)));
    }

    @Override
    public Mono<Config> getConfig() {
        return Mono.fromSupplier(() -> config);
    }

    @Override
    public Mono<DcOptions> getDcOptions() {
        return Mono.fromSupplier(() -> dcOptions);
    }

    @Override
    public Mono<PublicRsaKeyRegister> getPublicRsaKeyRegister() {
        return Mono.fromSupplier(() -> publicRsaKeyRegister);
    }

    // Updates methods
    // ==================

    @Override
    public Mono<Void> onNewMessage(Message update) {
        return Mono.fromRunnable(() -> saveMessage(update));
    }

    @Override
    public Mono<Message> onEditMessage(Message update) {
        return Mono.fromSupplier(() -> saveMessage(update));
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessages update) {
        return Mono.fromSupplier(() -> onDeleteMessages0(null, update.messages()));
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteScheduledMessages update) {
        return Mono.fromSupplier(() -> {
            Peer p = copyPeer(update.peer());
            return onDeleteMessages0(p, update.messages());
        });
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteChannelMessages update) {
        return Mono.fromSupplier(() -> {
            Peer p = ImmutablePeerChannel.of(update.channelId());
            return onDeleteMessages0(p, update.messages());
        });
    }

    @Nullable
    private ResolvedDeletedMessages onDeleteMessages0(@Nullable Peer peer, List<Integer> ids) {
        InputPeer inputPeer;
        if (peer == null) {
            inputPeer = ids.stream()
                    .flatMap(i -> Stream.ofNullable(messages.getIfPresent(new MessageId(i))))
                    .map(m -> {
                        Peer p;
                        if (m instanceof BaseMessage b) {
                            p = b.peerId();
                        } else if (m instanceof MessageService s) {
                            p = s.peerId();
                        } else {
                            throw new IllegalStateException();
                        }

                        return peers.getOrDefault(p, InputPeerEmpty.instance());
                    })
                    .findFirst()
                    .orElse(InputPeerEmpty.instance());
        } else {
            inputPeer = peers.getOrDefault(peer, InputPeerEmpty.instance());
        }

        long rawPeerId;
        switch (inputPeer.identifier()) {
            case InputPeerEmpty.ID -> {
                return null;
            }
            case InputPeerChannel.ID -> {
                var channel = (InputPeerChannel) inputPeer;
                rawPeerId = channel.channelId();
            }
            case InputPeerChannelFromMessage.ID -> {
                var minChannel = (InputPeerChannelFromMessage) inputPeer;
                rawPeerId = minChannel.channelId();
            }
            case InputPeerChat.ID, InputPeerSelf.ID, InputPeerUser.ID, InputPeerUserFromMessage.ID -> rawPeerId = -1;
            default -> throw new IllegalStateException("Unknown InputPeer type: " + inputPeer);
        }

        var messages = ids.stream()
                .flatMap(id -> Stream.ofNullable(this.messages.asMap().remove(new MessageId(rawPeerId, id))))
                .collect(Collectors.toList());

        return new ResolvedDeletedMessages(inputPeer, messages);
    }

    @Override
    public Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessages payload) {
        return Mono.fromRunnable(() -> onUpdatePinnedMessages0(copyPeer(payload.peer()), payload.pinned(), payload.messages()));
    }

    @Override
    public Mono<Void> onUpdatePinnedMessages(UpdatePinnedChannelMessages payload) {
        return Mono.fromRunnable(() -> onUpdatePinnedMessages0(ImmutablePeerChannel.of(payload.channelId()),
                payload.pinned(), payload.messages()));
    }

    protected void onUpdatePinnedMessages0(Peer peer, boolean pinned, List<Integer> ids) {
        long peerId = peer instanceof PeerChannel p ? p.channelId() : -1;
        for (int id : ids) {
            MessageId k = new MessageId(peerId, id);
            messages.asMap().computeIfPresent(k, (k1, v) -> {
                if (v instanceof BaseMessage b) {
                    return ImmutableBaseMessage.copyOf(b)
                            .withPinned(pinned);
                }
                return v;
            });
        }
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
                map.put(curr.userId(), copyChatParticipant(curr));
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

            if (payload instanceof BaseChatParticipants b) {
                var old = v.full.participants() instanceof BaseChatParticipants p ? p : null;

                // no update; just ignore received update
                // This check may create inconsistency
                if (old != null && b.version() < old.version()) {
                    return v;
                }

                var map = v.participants();
                map.clear();
                for (var p : b.participants()) {
                    var copy = copyChatParticipant(p);
                    map.put(copy.userId(), copy);
                }

                return v.withFull(f -> f.withParticipants(payload))
                        .withParticipants(map);
            } else if (payload instanceof ChatParticipantsForbidden f) {
                var updated = v.withFull(d -> d.withParticipants(payload));
                var self = f.selfParticipant();
                if (self != null) {
                    var map = updated.participants();
                    var copy = copyChatParticipant(self);
                    map.put(copy.userId(), copy);
                    updated = updated.withParticipants(map);
                }
                return updated;
            } else {
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
                map.put(getUserId(curr), copyChannelParticipant(curr));
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
    public Mono<Void> updateAuthKey(DataCenter dc, AuthKey authKey) {
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
                    var copy = copyChannelParticipant(p);
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
                var copy = copyChannelParticipant(payload.participant());
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
                case BaseMessages.ID -> {
                    var base = (BaseMessages) payload;
                    saveContacts(base.chats(), base.users());
                    for (var msg : base.messages()) {
                        saveMessage(msg);
                    }
                }
                case ChannelMessages.ID -> {
                    var channel = (ChannelMessages) payload;
                    saveContacts(channel.chats(), channel.users());
                    for (var msg : channel.messages()) {
                        saveMessage(msg);
                    }
                }
                case MessagesSlice.ID -> {
                    var slice = (MessagesSlice) payload;
                    saveContacts(slice.chats(), slice.users());
                    for (var msg : slice.messages()) {
                        saveMessage(msg);
                    }
                }
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

    protected void saveContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        for (Chat chat : chats) {
            saveChat(null, chat);
        }

        for (User user : users) {
            saveUser(null, user);
        }
    }

    protected void saveUser(@Nullable ImmutableUserFull anyUserFull, User anyUser) {
        if (!(anyUser instanceof BaseUser user)) {
            return;
        }

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

    protected void saveChat(@Nullable ChatFull anyChatFull, Chat anyChat) {
        switch (anyChat.identifier()) {
            case ChannelForbidden.ID -> {
                var copy = ImmutableChannelForbidden.copyOf((ChannelForbidden) anyChat);
                var inputPeer = ImmutableInputPeerChannel.of(copy.id(), copy.accessHash());
                peers.put(ImmutablePeerChannel.of(copy.id()), inputPeer);
                channels.compute(copy.id(), (k, v) -> {
                    var channelFull = Optional.ofNullable(anyChatFull)
                            .map(c -> ImmutableChannelFull.copyOf((ChannelFull) c))
                            .or(() -> Optional.ofNullable(v).map(c -> c.full))
                            .orElse(null);

                    return v == null
                            ? new ChannelInfo(copy, channelFull)
                            : v.withData(copy, channelFull);
                });
            }
            case ChatForbidden.ID -> {
                var copy = ImmutableChatForbidden.copyOf((ChatForbidden) anyChat);
                peers.putIfAbsent(ImmutablePeerChat.of(copy.id()), ImmutableInputPeerChat.of(copy.id()));
                chats.compute(copy.id(), (k, v) -> {
                    var chatFull = Optional.ofNullable(anyChatFull)
                            .map(c -> ImmutableBaseChatFull.copyOf((BaseChatFull) c))
                            .or(() -> Optional.ofNullable(v).map(c -> c.full))
                            .orElse(null);

                    ChatInfo updated = v == null
                            ? new ChatInfo(copy, chatFull)
                            : v.withData(copy, chatFull);

                    if (anyChatFull != null) { // was updated
                        if (chatFull.participants() instanceof BaseChatParticipants base) {

                            var map = updated.participants();
                            map.clear();
                            for (var p : base.participants()) {
                                var copyp = copyChatParticipant(p);
                                map.put(copyp.userId(), copyp);
                            }

                            updated = updated.withParticipants(map);
                        } else {
                            ChatParticipant selfParticipant;
                            if (chatFull.participants() instanceof ChatParticipantsForbidden f &&
                                    (selfParticipant = f.selfParticipant()) != null) {
                                var copyp = copyChatParticipant(selfParticipant);
                                var map = updated.participants();
                                map.put(copyp.userId(), copyp);
                                updated = updated.withParticipants(map);
                            } else {
                                updated = updated.withParticipants(null);
                            }
                        }
                    }

                    return updated;
                });
            }
            case BaseChat.ID -> {
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
                        if (chatFull.participants() instanceof BaseChatParticipants base) {

                            var map = updated.participants();
                            map.clear();
                            for (var p : base.participants()) {
                                var copy = copyChatParticipant(p);
                                map.put(copy.userId(), copy);
                            }

                            updated = updated.withParticipants(map);
                        } else {
                            ChatParticipant selfParticipant;
                            if (chatFull.participants() instanceof ChatParticipantsForbidden f &&
                                    (selfParticipant = f.selfParticipant()) != null) {
                                var copy = copyChatParticipant(selfParticipant);
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
            }
            case Channel.ID -> {
                var receivedChannel = (Channel) anyChat;
                var channelInfo = channels.get(receivedChannel.id());
                saveUsernamePeer(receivedChannel);

                // received channel is min, and we have non-min channel, just ignore received.
                if (receivedChannel.min() && channelInfo != null &&
                        (channelInfo.min instanceof ImmutableChannel c &&
                                !c.min() && c.accessHash() != null)) {
                    return;
                }
                var channelCopy = ImmutableChannel.copyOf(receivedChannel);
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
            }
            // if channel is min and received from message update,
            // then the *FromMessage peer would be saved in savePeer()
        }
    }

    protected static Message copyMessage(Message object) {
        if (object instanceof BaseMessage b) {
            return ImmutableBaseMessage.copyOf(b);
        } else if (object instanceof MessageService s) {
            return ImmutableMessageService.copyOf(s);
        } else { // MessageEmpty
            throw new IllegalArgumentException();
        }
    }

    protected static Peer copyPeer(Peer peer) {
        if (peer instanceof PeerUser p) {
            return ImmutablePeerUser.copyOf(p);
        } else if (peer instanceof PeerChannel p) {
            return ImmutablePeerChannel.copyOf(p);
        } else if (peer instanceof PeerChat p) {
            return ImmutablePeerChat.copyOf(p);
        } else {
            throw new IllegalStateException();
        }
    }

    protected static ChannelParticipant copyChannelParticipant(ChannelParticipant object) {
        return switch (object.identifier()) {
            case BaseChannelParticipant.ID -> ImmutableBaseChannelParticipant.copyOf((BaseChannelParticipant) object);
            case ChannelParticipantAdmin.ID -> ImmutableChannelParticipantAdmin.copyOf((ChannelParticipantAdmin) object);
            case ChannelParticipantBanned.ID -> ImmutableChannelParticipantBanned.copyOf((ChannelParticipantBanned) object);
            case ChannelParticipantCreator.ID -> ImmutableChannelParticipantCreator.copyOf((ChannelParticipantCreator) object);
            case ChannelParticipantLeft.ID -> ImmutableChannelParticipantLeft.copyOf((ChannelParticipantLeft) object);
            case ChannelParticipantSelf.ID -> ImmutableChannelParticipantSelf.copyOf((ChannelParticipantSelf) object);
            default -> throw new IllegalArgumentException("Unknown ChannelParticipant type: " + object);
        };
    }

    protected static ChatParticipant copyChatParticipant(ChatParticipant object) {
        return switch (object.identifier()) {
            case BaseChatParticipant.ID -> ImmutableBaseChatParticipant.copyOf((BaseChatParticipant) object);
            case ChatParticipantAdmin.ID -> ImmutableChatParticipantAdmin.copyOf((ChatParticipantAdmin) object);
            case ChatParticipantCreator.ID -> ImmutableChatParticipantCreator.copyOf((ChatParticipantCreator) object);
            default -> throw new IllegalArgumentException("Unknown ChatParticipant type: " + object);
        };
    }

    protected boolean isBot() {
        var userInfo = users.get(selfId());
        Objects.requireNonNull(userInfo);
        return userInfo.min.bot();
    }

    protected long selfId() {
        long id = selfId;
        if (id == 0) {
            throw new IllegalStateException("No information about current user.");
        }
        return id;
    }

    protected void saveUsernamePeer(TlObject object) {
        switch (object.identifier()) {
            case BaseUser.ID -> {
                var user = (BaseUser) object;
                String username = user.username();
                if (username != null) {
                    usernames.put(stripUsername(username), ImmutablePeerUser.of(user.id()));
                }
            }
            case Channel.ID -> {
                var channel = (Channel) object;
                String username = channel.username();
                if (username != null) {
                    usernames.put(stripUsername(username), ImmutablePeerChannel.of(channel.id()));
                }
            }
            default -> throw new IllegalStateException("Unexpected peer type: " + object);
        }
    }

    protected void savePeer0(Peer p, Peer peerId, int msgId) {
        switch (p.identifier()) {
            case PeerChat.ID -> {
                var cp = ImmutablePeerChat.copyOf((PeerChat) p);
                peers.putIfAbsent(cp, ImmutableInputPeerChat.of(cp.chatId()));
            }
            // Here only handling for min objects
            case PeerChannel.ID -> {
                var chp = ImmutablePeerChannel.copyOf((PeerChannel) p);
                var channelInfo = channels.get(chp.channelId());

                if ((channelInfo == null || channelInfo.min instanceof ImmutableChannel c && c.min()) && !isBot()) {
                    var chatPeer = peers.get(peerId);
                    if (chatPeer == null) {
                        break;
                    }

                    var minChannel = ImmutableInputPeerChannelFromMessage.of(
                            chatPeer, msgId, chp.channelId());
                    peers.put(chp, minChannel);
                }
            }
            case PeerUser.ID -> {
                var up = ImmutablePeerUser.copyOf((PeerUser) p);
                var userInfo = users.get(up.userId());
                if ((userInfo == null || userInfo.min.min()) && !isBot()) {
                    InputPeer chatPeer = peers.get(peerId);
                    if (chatPeer == null) {
                        break;
                    }

                    var minUser = ImmutableInputPeerUserFromMessage.of(
                            chatPeer, msgId, up.userId());
                    peers.put(up, minUser);
                }
            }
            default -> throw new IllegalStateException("Unexpected Peer type: " + p);
        }
    }

    protected void savePeer(Peer p, MessageService message) {
        savePeer0(p, message.peerId(), message.id());
    }

    protected void savePeer(Peer p, BaseMessage message) {
        savePeer0(p, message.peerId(), message.id());
    }

    protected void addContact(Peer p, Consumer<Chat> chats, Consumer<User> users) {
        switch (p.identifier()) {
            case PeerChat.ID -> {
                var cp = (PeerChat) p;
                var chatInfo = this.chats.get(cp.chatId());
                if (chatInfo != null) {
                    chats.accept(chatInfo.min);
                }
            }
            case PeerChannel.ID -> {
                var chp = (PeerChannel) p;
                var channelInfo = this.channels.get(chp.channelId());
                if (channelInfo != null) {
                    chats.accept(channelInfo.min);
                }
            }
            case PeerUser.ID -> {
                var up = (PeerUser) p;
                var userInfo = this.users.get(up.userId());
                if (userInfo != null) {
                    users.accept(userInfo.min);
                }
            }
            default -> throw new IllegalArgumentException("Unknown Peer type: " + p);
        }
    }

    protected void addContact(Peer peer, Collection<Chat> chats, Collection<User> users) {
        addContact(peer, chats::add, users::add);
    }

    @Nullable
    protected Message saveMessage(Message message) {
        Message old;
        if (message instanceof BaseMessage b) {
            MessageId key = MessageId.create(b);
            var copy = ImmutableBaseMessage.copyOf(b);
            old = messages.asMap().put(key, copy);

            // TODO: extract all possible peers from message?
            savePeer(copy.peerId(), copy);
            Peer p = copy.fromId();
            if (p != null) {
                savePeer(p, copy);
            }

            MessageMedia media = copy.media();
            if (media != null) {
                switch (media.identifier()) {
                    case MessageMediaPoll.ID -> {
                        var mmp = (MessageMediaPoll) media;
                        polls.put(mmp.poll().id(), new MessagePoll(mmp.poll(), copy.peerId(), copy.id()));
                    }
                }
            }
        } else if (message instanceof MessageService m) {
            MessageId key = MessageId.create(m);
            var copy = ImmutableMessageService.copyOf(m);
            old = messages.asMap().put(key, copy);

            savePeer(copy.peerId(), copy);
            Peer p = copy.fromId();
            if (p != null) {
                savePeer(p, copy);
            }
        } else {
            throw new IllegalStateException();
        }

        return old;
    }

    protected static class ChannelInfo {
        protected final Chat min; // ImmutableChannel or ImmutableChannelForbidden
        @Nullable
        protected final ImmutableChannelFull full;
        @Nullable // initializes on demand
        protected final ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants;

        protected ChannelInfo(Chat min, @Nullable ImmutableChannelFull full) {
            this(min, full, null);
        }

        protected ChannelInfo(Chat min, @Nullable ImmutableChannelFull full,
                    @Nullable ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants) {
            this.min = Objects.requireNonNull(min);
            this.full = full;
            this.participants = participants;
        }

        protected ChannelInfo withParticipants(@Nullable ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants) {
            if (this.participants == participants) return this;
            return new ChannelInfo(min, full, participants);
        }

        protected ConcurrentMap<Peer, telegram4j.tl.ChannelParticipant> participants() {
            return participants != null ? participants : new ConcurrentHashMap<>();
        }

        protected ChannelInfo withFull(UnaryOperator<ImmutableChannelFull> mapper) {
            if (this.full == null) return this;
            var full = mapper.apply(this.full);
            if (this.full == full) return this;
            return new ChannelInfo(min, full, participants);
        }

        protected ChannelInfo withData(Chat min, @Nullable ImmutableChannelFull full) {
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

    protected static class ChatInfo {
        protected final Chat min; // ImmutableBaseChat or ImmutableChatForbidden
        @Nullable
        protected final ImmutableBaseChatFull full;
        @Nullable // initializes on demand
        protected final ConcurrentMap<Long, ChatParticipant> participants;

        protected ChatInfo(Chat min, @Nullable ImmutableBaseChatFull full) {
            this(min, full, null);
        }

        protected ChatInfo(Chat min, @Nullable ImmutableBaseChatFull full,
                 @Nullable ConcurrentMap<Long, ChatParticipant> participants) {
            this.min = Objects.requireNonNull(min);
            this.full = full;
            this.participants = participants;
        }

        protected ChatInfo withFull(UnaryOperator<ImmutableBaseChatFull> mapper) {
            if (this.full == null) return this;
            var full = mapper.apply(this.full);
            if (this.full == full) return this;
            return new ChatInfo(min, full, participants);
        }

        protected ChatInfo withParticipants(@Nullable ConcurrentMap<Long, ChatParticipant> participants) {
            if (this.participants == participants) return this;
            return new ChatInfo(min, full, participants);
        }

        protected ConcurrentMap<Long, ChatParticipant> participants() {
            return participants != null ? participants : new ConcurrentHashMap<>();
        }

        public ChatInfo withData(Chat min, @Nullable ImmutableBaseChatFull full) {
            if (this.min == min && this.full == full) return this;
            return new ChatInfo(min, full, participants);
        }
    }

    protected static class MessageId implements Comparable<MessageId> {
        protected final long chatId; // -1 for DM/Group Chats
        protected final int messageId;

        protected static MessageId create(Peer peerId, int messageId) {
            long chatId = peerId instanceof PeerChannel c ? c.channelId() : -1;
            return new MessageId(chatId, messageId);
        }

        protected static MessageId create(BaseMessage message) {
            return create(message.peerId(), message.id());
        }

        protected static MessageId create(MessageService message) {
            return create(message.peerId(), message.id());
        }

        MessageId(int messageId) {
            this(-1, messageId);
        }

        MessageId(long chatId, int messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        @Override
        public int compareTo(MessageId o) {
            int c = Long.compare(chatId, o.chatId);
            if (c != 0) return c;
            return Integer.compare(messageId, o.messageId);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof MessageId m)) return false;
            return chatId == m.chatId && messageId == m.messageId;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(chatId) ^ messageId;
        }
    }

    protected static class PartialFields<M, F> {
        final M min;
        @Nullable
        final F full;

        PartialFields(M min, @Nullable F full) {
            this.min = Objects.requireNonNull(min);
            this.full = full;
        }
    }

    protected static class DcKey implements Comparable<DcKey> {
        protected final boolean test;
        protected final DataCenter.Type type;
        protected final int id;

        DcKey(boolean test, DataCenter.Type type, int id) {
            this.test = test;
            this.type = type;
            this.id = id;
        }

        protected static DcKey create(DataCenter dc) {
            return new DcKey(dc.isTest(), dc.getType(), dc.getId());
        }

        @Override
        public int compareTo(DcKey o) {
            int c = Boolean.compare(test, o.test);
            if (c != 0) return c;
            c = type.compareTo(o.type);
            if (c != 0) return c;
            return Integer.compare(id, o.id);
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
