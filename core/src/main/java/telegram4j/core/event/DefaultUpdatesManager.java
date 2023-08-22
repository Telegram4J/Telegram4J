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
package telegram4j.core.event;

import org.reactivestreams.Publisher;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.dispatcher.UpdateContext;
import telegram4j.core.event.dispatcher.UpdatesMapper;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.Message;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;
import telegram4j.core.util.Timeout;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.RpcException;
import telegram4j.tl.*;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.GetState;
import telegram4j.tl.request.updates.ImmutableGetChannelDifference;
import telegram4j.tl.request.updates.ImmutableGetDifference;
import telegram4j.tl.updates.*;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static telegram4j.core.internal.MappingUtil.getAuthor;
import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

/** Manager for correct and complete work with general and channel updates. */
public class DefaultUpdatesManager implements UpdatesManager {
    protected static final Logger log = Loggers.getLogger(DefaultUpdatesManager.class);

    protected static final VarHandle REQUESTING_DIFFERENCE;

    static {
        var lookup = MethodHandles.lookup();
        try {
            REQUESTING_DIFFERENCE = lookup.findVarHandle(DefaultUpdatesManager.class, "requestingDifference", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected final Timeout stateTimeout = Timeout.create(Schedulers.single(),
            Sinks.many().unicast().onBackpressureError());

    protected final MTProtoTelegramClient client;
    protected final Options options;

    protected volatile int pts = -1;
    protected volatile int qts = -1;
    protected volatile int date = -1;
    protected volatile int seq = -1;
    protected volatile boolean initialized;

    protected volatile boolean requestingDifference;

    public DefaultUpdatesManager(MTProtoTelegramClient client, Options options) {
        this.client = Objects.requireNonNull(client);
        this.options = Objects.requireNonNull(options);
    }

    @Override
    public Mono<Void> start() {
        return stateTimeout.asFlux()
                .concatMap(t -> fillGap())
                .then();
    }

    @Override
    public Mono<Void> fillGap() {
        return client.getMtProtoClientGroup()
        .send(DcId.main(), GetState.instance())
        .flatMap(state -> {
            if (!initialized) {
                return client.getMtProtoResources().getStoreLayout()
                        .getCurrentState()
                        .defaultIfEmpty(state)
                        .doOnNext(s -> {
                            applyStateLocal(s);
                            initialized = true;
                        });
            }
            return Mono.empty();
        })
        .thenMany(getDifference())
        .doOnNext(client.getMtProtoResources()
                .getEventDispatcher()::publish)
        .then();
    }

    @Override
    public Flux<Event> handle(Updates updates) {
        stateTimeout.restart(options.checkin);

        return switch (updates.identifier()) {
            case UpdatesTooLong.ID -> getDifference();
            case UpdateShort.ID -> {
                var data = (UpdateShort) updates;

                if (log.isDebugEnabled()) {
                    log.debug("Updating state, date: {}->{}", Instant.ofEpochSecond(date), Instant.ofEpochSecond(data.date()));
                }
                date = data.date();

                yield saveStateIf(true)
                        .thenMany(applyUpdate(UpdateContext.create(client, data.update()), true));
            }
            case BaseUpdates.ID -> {
                var data = (BaseUpdates) updates;

                StringJoiner j = new StringJoiner(", ");
                int seqEnd = data.seq();
                if (seqEnd != 0) {
                    int seq = this.seq;

                    if (seq + 1 < seqEnd) {
                        log.debug("Updates gap found. Received seq: {}-{}, local seq: {}", seqEnd, seqEnd, seq);

                        yield getDifference();
                    } else if (seq + 1 > seqEnd) {
                        yield Flux.empty();
                    }

                    j.add("seq: " + seq + "->" + seqEnd);
                    this.seq = seqEnd;
                }

                if (log.isDebugEnabled()) {
                    j.add("date: " + Instant.ofEpochSecond(date) + "->" + Instant.ofEpochSecond(data.date()));
                    log.debug("Updating state, " + j);
                }
                date = data.date();

                yield handleUpdates(List.of(), data.updates(), data.chats(), data.users(), true);
            }
            case UpdatesCombined.ID -> {
                var data = (UpdatesCombined) updates;

                int seqBegin = data.seqStart();
                int seqEnd = data.seq();
                StringJoiner j = new StringJoiner(", ");
                if (seqBegin != 0 && seqEnd != 0) {
                    int seq = this.seq;

                    if (seq + 1 < seqBegin) {
                        log.debug("Updates gap found. Received seq: {}-{}, local seq: {}", seqBegin, seqEnd, seq);

                        yield getDifference();
                    } else if (seq + 1 > seqBegin) {
                        yield Flux.empty();
                    }

                    j.add("seq: " + seq + "->" + seqEnd);
                    this.seq = seqEnd;
                }

                if (log.isDebugEnabled()) {
                    j.add("date: " + Instant.ofEpochSecond(date) + "->" + Instant.ofEpochSecond(data.date()));
                    log.debug("Updating state, " + j);
                }
                date = data.date();

                yield handleUpdates(List.of(), data.updates(), data.chats(), data.users(), true);
            }
            case UpdateShortChatMessage.ID -> {
                var data = (UpdateShortChatMessage) updates;

                int pts = this.pts;
                if (pts + data.ptsCount() < data.pts()) {
                    log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                            data.pts() - data.ptsCount(), data.pts(), pts);
                    yield getDifference(pts, qts, date);
                } else if (pts + data.ptsCount() > data.pts()) {
                    yield Flux.empty();
                }

                if (options.discardMinimalMessageUpdates) {
                    yield getDifference(pts, qts, date);
                }

                log.debug("Updating state, pts: {}->{}", pts, data.pts());
                this.pts = data.pts();

                var mapUpdate = UpdatesMapper.instance.handle(UpdateContext.create(client, UpdateNewMessage.builder()
                        .message(BaseMessage.builder()
                                .flags(data.flags())
                                .id(data.id())
                                .date(data.date())
                                .fromId(ImmutablePeerUser.of(data.fromId()))
                                .peerId(ImmutablePeerChat.of(data.chatId()))
                                .fwdFrom(data.fwdFrom())
                                .viaBotId(data.viaBotId())
                                .replyTo(data.replyTo())
                                .entities(data.entities())
                                .ttlPeriod(data.ttlPeriod())
                                .message(data.message())
                                .build())
                        .pts(data.pts())
                        .ptsCount(data.ptsCount())
                        .build()));

                yield saveStateIf(true)
                        .thenMany(mapUpdate);
            }
            case UpdateShortMessage.ID -> {
                var data = (UpdateShortMessage) updates;

                int pts = this.pts;
                if (pts + data.ptsCount() < data.pts()) {
                    log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                            data.pts() - data.ptsCount(), data.pts(), pts);
                    yield getDifference(pts, qts, date);
                } else if (pts + data.ptsCount() > data.pts()) {
                    yield Flux.empty();
                }

                if (options.discardMinimalMessageUpdates) {
                    yield getDifference(pts, qts, date);
                }

                log.debug("Updating state, pts: {}->{}", pts, data.pts());
                this.pts = data.pts();

                var mapUpdate = UpdatesMapper.instance.handle(UpdateContext.create(client, UpdateNewMessage.builder()
                        .message(BaseMessage.builder()
                                .flags(data.flags())
                                .id(data.id())
                                .date(data.date())
                                .fwdFrom(data.fwdFrom())
                                .viaBotId(data.viaBotId())
                                .replyTo(data.replyTo())
                                .entities(data.entities())
                                .ttlPeriod(data.ttlPeriod())
                                .message(data.message())
                                .peerId(ImmutablePeerUser.of(data.userId()))
                                .fromId(data.out()
                                        ? ImmutablePeerUser.of(client.getSelfId().asLong())
                                        : ImmutablePeerUser.of(data.userId()))
                                .build())
                        .pts(data.pts())
                        .ptsCount(data.ptsCount())
                        .build()));

                yield saveStateIf(true)
                        .thenMany(mapUpdate);
            }
            default -> Flux.error(new IllegalArgumentException("Unknown Updates type: " + updates));
        };
    }

    @Override
    public Mono<Void> close() {
        return Mono.fromRunnable(stateTimeout::close);
    }

    protected Mono<Void> saveStateIf(boolean needSave) {
        if (!needSave) {
            return Mono.empty();
        }

        return Mono.defer(() -> client.getMtProtoResources()
                .getStoreLayout()
                .updateState(ImmutableState.of(pts, qts, date, seq, -1)));
    }

    protected void applyStateLocal(State state) {
        pts = state.pts();
        qts = state.qts();
        date = state.date();
        seq = state.seq();
    }

    protected Mono<Void> applyState(State state, boolean intermediate) {
        return Mono.defer(() -> {
            if (log.isDebugEnabled()) {
                StringJoiner j = new StringJoiner(", ");
                int pts = this.pts;
                if (pts != state.pts()) {
                    j.add("pts: " + pts + "->" + state.pts());
                }
                int qts = this.qts;
                if (qts != state.qts()) {
                    j.add("qts: " + qts + "->" + state.qts());
                }
                int seq = this.seq;
                if (seq != state.seq()) {
                    j.add("seq: " + seq + "->" + state.seq());
                }
                int date = this.date;
                if (date != state.date()) {
                    j.add("date: " + Instant.ofEpochSecond(date) + "->" + Instant.ofEpochSecond(state.date()));
                }

                j.add("unread count: " + state.unreadCount());
                log.debug("Updating state" + (intermediate ? " to intermediate" : "") + ", " + j);
            }

            applyStateLocal(state);
            return client.getMtProtoResources()
                    .getStoreLayout()
                    .updateState(state);
        });
    }

    protected Flux<Event> getDifference() {
        return Flux.defer(() -> getDifference(pts, qts, date));
    }

    protected Flux<Event> getDifference(int pts, int qts, int date) {
        if (pts == -1 || qts == -1 || date <= 0) {
            if (log.isWarnEnabled()) {
                log.warn("Incorrect get difference parameters, pts: {}, qts: {}, date: {}", pts, qts, date);
            }
            return Flux.empty();
        }

        if (!requestingDifference && REQUESTING_DIFFERENCE.compareAndSet(this, false, true)) {
            if (log.isDebugEnabled()) {
                log.debug("Getting difference, pts: {}, qts: {}, date: {}", pts, qts, Instant.ofEpochSecond(date));
            }

            stateTimeout.restart(options.checkin);

            return client.getMtProtoClientGroup()
                    .send(DcId.main(), ImmutableGetDifference.of(pts, date, qts))
                    .flatMapMany(this::handleDifference)
                    .doOnTerminate(() -> requestingDifference = false);
        }
        return Flux.empty();
    }

    private Publisher<Event> handleDifference(Difference difference) {
        if (log.isTraceEnabled()) {
            log.trace("difference: {}", difference);
        }

        return switch (difference.identifier()) {
            case DifferenceEmpty.ID -> {
                var diff = (DifferenceEmpty) difference;

                boolean updated = false;
                if (log.isDebugEnabled()) {
                    StringJoiner j = new StringJoiner(", ");
                    int seq = this.seq;
                    if (seq != diff.seq()) {
                        j.add("seq: " + seq + "->" + diff.seq());
                        this.seq = diff.seq();
                    }
                    int currDate = this.date;
                    if (currDate != diff.date()) {
                        j.add("date: " + Instant.ofEpochSecond(currDate) + "->" + Instant.ofEpochSecond(diff.date()));
                        this.date = diff.date();
                    }

                    String str = j.toString();
                    if (str.isEmpty()) {
                        yield Flux.empty();
                    }

                    updated = true;
                    log.debug("Updating state, " + j);
                } else {
                    int seq = this.seq;
                    if (seq != diff.seq()) {
                        this.seq = diff.seq();
                        updated = true;
                    }
                    int currDate = this.date;
                    if (currDate != diff.date()) {
                        this.date = diff.date();
                        updated = true;
                    }

                    this.seq = diff.seq();
                    this.date = diff.date();
                }

                yield saveStateIf(updated)
                        .then(Mono.empty());
            }
            case BaseDifference.ID -> {
                var diff = (BaseDifference) difference;

                yield applyState(diff.state(), false)
                        .thenMany(handleUpdates(diff.newMessages(), diff.otherUpdates(),
                                diff.chats(), diff.users(), false));
            }
            case DifferenceSlice.ID -> {
                var diff = (DifferenceSlice) difference;
                State state = diff.intermediateState();

                yield applyState(state, true)
                        .thenMany(handleUpdates(diff.newMessages(), diff.otherUpdates(),
                                diff.chats(), diff.users(), false))
                        .concatWith(getDifference(state.pts(), state.qts(), state.date()));
            }
            // TODO DifferenceTooLong ?
            default -> Mono.error(new IllegalArgumentException("Unknown difference type: " + difference));
        };
    }

    @Nullable
    private static Variant2<BaseMessage, MessageService> filterMessage(telegram4j.tl.Message message) {
        if (message instanceof BaseMessage b) {
            return Variant2.ofT1(b);
        } else if (message instanceof MessageService m) {
            return Variant2.ofT2(m);
        } else { // MessageEmpty
            return null;
        }
    }

    protected Flux<Event> handleUpdates(List<telegram4j.tl.Message> newMessages, List<Update> otherUpdates,
                                        List<telegram4j.tl.Chat> chats, List<telegram4j.tl.User> users,
                                        boolean notFromDiff) {
        var usersMap = users.stream()
                .flatMap(u -> Stream.ofNullable(EntityFactory.createUser(client, u)))
                .collect(Collectors.toMap(User::getId, Function.identity()));

        var selfUser = usersMap.get(client.getSelfId());

        var chatsMap = chats.stream()
                .flatMap(u -> Stream.ofNullable(EntityFactory.createChat(client, u, null)))
                .collect(Collectors.toMap(Chat::getId, Function.identity()));

        var messageCreateEvents = Flux.fromIterable(newMessages)
                .mapNotNull(DefaultUpdatesManager::filterMessage)
                .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                        .getStoreLayout().existMessage(message.map(BaseMessage::peerId, MessageService::peerId),
                                message.map(BaseMessage::id, MessageService::id))))
                .flatMap(data -> {
                    Id peerId = Id.of(data.map(BaseMessage::peerId, MessageService::peerId));
                    var chat = getChatEntity(peerId, chatsMap, usersMap, selfUser);
                    var author = getAuthor(data, chat, client, chatsMap, usersMap).orElse(null);

                    // TODO: this is unnecessary but required for file contexts
                    return Mono.justOrEmpty(chat)
                            .map(Chat::getId)
                            .switchIfEmpty(client.asInputPeerExact(peerId)
                                    .map(p -> Id.of(p, client.getSelfId())))
                            .map(i -> new Message(client, data, i))
                            .map(m -> new SendMessageEvent(client, m, chat, author));
                });

        var applyChannelDifference = Flux.fromIterable(otherUpdates)
                .ofType(UpdateChannelTooLong.class)
                // TODO use UpdateChannelTooLong#pts()
                .flatMap(u -> client.getMtProtoResources().getStoreLayout()
                        .getChannelFullById(u.channelId())
                        .switchIfEmpty(client.getMtProtoResources().getStoreLayout()
                                .resolveChannel(u.channelId())
                                .flatMap(client.getServiceHolder().getChatService()::getFullChannel)
                                .then(Mono.empty())) // no channel pts; can't request channel updates
                        .onErrorResume(RpcException.isErrorMessage("CHANNEL_PRIVATE"), e -> Mono.empty())
                        .map(chatFull -> (ChannelFull) chatFull.fullChat())
                        .map(i -> Tuples.of(u, i.pts())))
                .filter(TupleUtils.predicate((u, c) -> Optional.ofNullable(u.pts()).map(i -> i > c).orElse(true)))
                .flatMap(TupleUtils.function((u, cpts) -> {
                    var id = Optional.ofNullable(chatsMap.get(Id.ofChannel(u.channelId())))
                            .map(c -> client.asResolvedInputChannel(c.getId())) // must be present
                            .orElseThrow();

                    Integer upts = (upts = u.pts()) == null ? -1 : upts;
                    int dpts = Math.max(1, Math.min(upts, cpts));
                    int limit = options.channelDifferenceLimit;

                    if (log.isDebugEnabled()) {
                        log.debug("Getting channel difference, channel: {}, pts: {}, limit: {}", u.channelId(), dpts, limit);
                    }

                    var request = GetChannelDifference.builder()
                            .force(false)
                            .channel(id)
                            .pts(dpts)
                            .filter(ChannelMessagesFilterEmpty.instance())
                            .limit(limit)
                            .build();

                    return client.getMtProtoClientGroup()
                            .send(DcId.main(), request)
                            .flatMapMany(diff -> handleChannelDifference(request, diff));
                }));

        var concatedUpdates = Flux.fromIterable(otherUpdates)
                .map(u -> UpdateContext.create(client, chatsMap, usersMap, u))
                .flatMap(u -> applyUpdate(u, notFromDiff))
                .concatWith(messageCreateEvents)
                .concatWith(applyChannelDifference);

        return client.getMtProtoResources()
                .getStoreLayout().onContacts(chats, users)
                .thenMany(concatedUpdates);
    }

    protected Flux<Event> handleChannelDifference(GetChannelDifference request, ChannelDifference diff) {
        Id channelId = Id.of(request.channel(), client.getSelfId());
        if (log.isTraceEnabled()) {
            log.trace("channel difference for {}: {}", channelId.asString(), diff);
        }

        int newPts = switch (diff.identifier()) {
            case BaseChannelDifference.ID -> ((BaseChannelDifference) diff).pts();
            case ChannelDifferenceEmpty.ID -> ((ChannelDifferenceEmpty) diff).pts();
            case ChannelDifferenceTooLong.ID -> {
                ChannelDifferenceTooLong diff0 = (ChannelDifferenceTooLong) diff;
                if (diff0.dialog() instanceof BaseDialog d) {
                    Integer pts = d.pts();
                    yield pts != null ? pts : 1;
                } else {
                    yield 1;
                }
            }
            default -> throw new IllegalArgumentException("Unknown channel difference type: " + diff);
        };

        Mono<Void> updatePts = Mono.defer(() -> {
            if (log.isDebugEnabled()) {
                log.debug("Updating state for channel: {}, pts: {}->{}", channelId.asString(), request.pts(), newPts);
            }

            return client.getMtProtoResources()
                    .getStoreLayout().updateChannelPts(channelId.asLong(), newPts);
        });

        Flux<Event> refetchDifference = Flux.defer(() -> {
            if (diff.isFinal()) {
                return Flux.empty();
            }

            if (log.isDebugEnabled()) {
                log.debug("Getting non-final channel difference, channel: {}, pts: {}, limit: {}",
                        channelId.asString(), newPts, request.limit());
            }

            var updRequest = ImmutableGetChannelDifference.copyOf(request)
                    .withPts(newPts);

            return client.getMtProtoClientGroup()
                    .send(DcId.main(), updRequest)
                    .flatMapMany(d -> handleChannelDifference(updRequest, d));
        });

        return switch (diff.identifier()) {
            case BaseChannelDifference.ID -> {
                var diff0 = (BaseChannelDifference) diff;

                var usersMap = diff0.users().stream()
                        .flatMap(u -> Stream.ofNullable(EntityFactory.createUser(client, u)))
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                var selfUser = usersMap.get(client.getSelfId());

                var chatsMap = diff0.chats().stream()
                        .flatMap(u -> Stream.ofNullable(EntityFactory.createChat(client, u, null)))
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                Flux<Event> messageCreateEvents = Flux.fromIterable(diff0.newMessages())
                        .mapNotNull(DefaultUpdatesManager::filterMessage)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message.map(BaseMessage::peerId, MessageService::peerId),
                                        message.map(BaseMessage::id, MessageService::id))))
                        .map(data -> {
                            var chat = getChatEntity(Id.of(data.map(BaseMessage::peerId, MessageService::peerId)), chatsMap, usersMap, selfUser);
                            var author = getAuthor(data, chat, client, chatsMap, usersMap).orElse(null);
                            var msg = new Message(client, data, channelId);

                            return new SendMessageEvent(client, msg, chat, author);
                        });

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(diff0.chats(), diff0.users());

                yield updatePts.and(saveContacts)
                        .thenMany(Flux.fromIterable(diff0.otherUpdates()))
                        .flatMap(update -> UpdatesMapper.instance.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)))
                        .concatWith(messageCreateEvents)
                        .concatWith(refetchDifference);
            }
            case ChannelDifferenceEmpty.ID -> {
                var diff0 = (ChannelDifferenceEmpty) diff;

                yield updatePts.thenMany(Flux.empty());
            }
            case ChannelDifferenceTooLong.ID -> {
                var diff0 = (ChannelDifferenceTooLong) diff;

                var usersMap = diff0.users().stream()
                        .flatMap(u -> Stream.ofNullable(EntityFactory.createUser(client, u)))
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                var selfUser = usersMap.get(client.getSelfId());

                var chatsMap = diff0.chats().stream()
                        .flatMap(u -> Stream.ofNullable(EntityFactory.createChat(client, u, null)))
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                Flux<Event> messageCreateEvents = Flux.fromIterable(diff0.messages())
                        .mapNotNull(DefaultUpdatesManager::filterMessage)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message.map(BaseMessage::peerId, MessageService::peerId),
                                        message.map(BaseMessage::id, MessageService::id))))
                        .map(data -> {
                            var chat = getChatEntity(Id.of(data.map(BaseMessage::peerId, MessageService::peerId)), chatsMap, usersMap, selfUser);
                            var author = getAuthor(data, chat, client, chatsMap, usersMap).orElse(null);
                            var msg = new Message(client, data, channelId);

                            return new SendMessageEvent(client, msg, chat, author);
                        });

                var saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(diff0.chats(), diff0.users());

                yield updatePts.and(saveContacts)
                        .thenMany(messageCreateEvents)
                        .concatWith(refetchDifference);
            }
            default -> Flux.error(new IllegalArgumentException("Unknown channel difference type: " + diff));
        };
    }

    protected static boolean isCommonPtsUpdate(Update update) {
        // https://github.com/tdlib/td/blob/89efc4feca1a3cfbcc516fbc02b84420f9620a53/td/telegram/UpdatesManager.cpp#L3264
        return switch (update.identifier()) {
            case UpdateNewMessage.ID:
            case UpdateReadMessagesContents.ID:
            case UpdateEditMessage.ID:
            case UpdateDeleteMessages.ID:
            case UpdateReadHistoryInbox.ID:
            case UpdateReadHistoryOutbox.ID:
            case UpdateWebPage.ID:
            case UpdatePinnedMessages.ID:
            case UpdateFolderPeers.ID:
                yield true;
            default:
                yield false;
        };
    }

    protected static boolean isChannelPtsUpdate(Update update) {
        // https://github.com/tdlib/td/blob/89efc4feca1a3cfbcc516fbc02b84420f9620a53/td/telegram/UpdatesManager.cpp#L3339
        return switch (update.identifier()) {
            case UpdateNewChannelMessage.ID:
            case UpdateEditChannelMessage.ID:
            case UpdateDeleteChannelMessages.ID:
            case UpdatePinnedChannelMessages.ID:
                yield true;
            default:
                yield false;
        };
    }

    protected static boolean isQtsUpdate(Update update) {
        // https://github.com/tdlib/td/blob/89efc4feca1a3cfbcc516fbc02b84420f9620a53/td/telegram/UpdatesManager.cpp#L3306
        return switch (update.identifier()) {
            case UpdateNewEncryptedMessage.ID:
            case UpdateMessagePollVote.ID:
            case UpdateBotStopped.ID:
            case UpdateChatParticipant.ID:
            case UpdateChannelParticipant.ID:
            case UpdateBotChatInviteRequester.ID:
                yield true;
            default:
                yield false;
        };
    }

    protected static boolean isBotQtsUpdate(Update update) {
        return switch (update.identifier()) {
            case UpdateMessagePollVote.ID:
            case UpdateBotStopped.ID:
            case UpdateChatParticipant.ID:
            case UpdateChannelParticipant.ID:
            case UpdateBotChatInviteRequester.ID:
                yield true;
            default:
                yield false;
        };
    }

    // only for channel updates
    private static long getChannelId(telegram4j.tl.Message message) {
        return switch (message.identifier()) {
            case BaseMessage.ID -> {
                var m = (BaseMessage) message;
                yield ((PeerChannel) m.peerId()).channelId();
            }
            case MessageService.ID -> {
                var m = (MessageService) message;
                yield ((PeerChannel) m.peerId()).channelId();
            }
            case MessageEmpty.ID -> {
                var m = (MessageEmpty) message;
                var peer = (PeerChannel) m.peerId();
                if (peer == null) {
                    yield -1;
                }
                yield peer.channelId();
            }
            default -> throw new IllegalStateException("Unexpected channel message type: " + message);
        };
    }

    private Flux<Event> applyUpdate(UpdateContext<Update> ctx, boolean notFromDiff) {
        Flux<Event> mapUpdate = UpdatesMapper.instance.handle(ctx);

        Update u = ctx.getUpdate();
        if (isCommonPtsUpdate(u)) {
            int pts;
            int ptsCount;
            // region pts, ptsCount extraction
            switch (u.identifier()) {
                case UpdateNewMessage.ID -> {
                    var c = (UpdateNewMessage) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdateReadMessagesContents.ID -> {
                    var c = (UpdateReadMessagesContents) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdateEditMessage.ID -> {
                    var c = (UpdateEditMessage) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdateDeleteMessages.ID -> {
                    var c = (UpdateDeleteMessages) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdateReadHistoryInbox.ID -> {
                    var c = (UpdateReadHistoryInbox) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdateReadHistoryOutbox.ID -> {
                    var c = (UpdateReadHistoryOutbox) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdateWebPage.ID -> {
                    var c = (UpdateWebPage) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdatePinnedMessages.ID -> {
                    var c = (UpdatePinnedMessages) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                case UpdateFolderPeers.ID -> {
                    var c = (UpdateFolderPeers) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                }
                default -> throw new IllegalStateException("Unexpected common pts update type: " + u);
            }
            // endregion

            int localPts = this.pts;
            if (localPts + ptsCount < pts) {
                log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                        pts - ptsCount, pts, localPts);

                return getDifference(localPts, qts, date);
            } else if (localPts + ptsCount > pts) {
                return Flux.empty();
            } else {
                if (log.isDebugEnabled() && notFromDiff) {
                    log.debug("Updating state, pts: {}->{}", localPts, pts);
                }

                this.pts = pts;

                return saveStateIf(true)
                        .thenMany(mapUpdate);
            }
        } else if (isChannelPtsUpdate(u)) {
            int pts;
            int ptsCount;
            long channelId;
            // region pts, ptsCount, channelId extraction
            switch (u.identifier()) {
                case UpdateNewChannelMessage.ID -> {
                    var c = (UpdateNewChannelMessage) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                    channelId = getChannelId(c.message());
                }
                case UpdateEditChannelMessage.ID -> {
                    var c = (UpdateEditChannelMessage) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                    channelId = getChannelId(c.message());
                }
                case UpdateDeleteChannelMessages.ID -> {
                    var c = (UpdateDeleteChannelMessages) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                    channelId = c.channelId();
                }
                case UpdatePinnedChannelMessages.ID -> {
                    var c = (UpdatePinnedChannelMessages) u;
                    pts = c.pts();
                    ptsCount = c.ptsCount();
                    channelId = c.channelId();
                }
                default -> throw new IllegalStateException("Unexpected channel pts update type: " + u);
            }
            // endregion

            if (channelId == -1) {
                return Flux.empty();
            }

            return client.getMtProtoResources()
                    .getStoreLayout().getChannelFullById(channelId)
                    .switchIfEmpty(Mono.defer(() -> client.getMtProtoResources()
                            .getStoreLayout().resolveChannel(channelId)
                            .flatMap(client.getServiceHolder().getChatService()::getFullChannel)
                            .onErrorResume(RpcException.isErrorMessage("CHANNEL_PRIVATE"), e -> Mono.empty())))
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .flatMapMany(chatFullOpt -> {
                        var chatFull = chatFullOpt.orElse(null);
                        if (chatFull == null) {
                            if (log.isDebugEnabled() && notFromDiff) {
                                log.debug("Updating state for channel: {}, pts: unknown->{}", channelId, pts);
                            }
                            return mapUpdate;
                        }

                        if (!(chatFull.fullChat() instanceof ChannelFull f)) {
                            return Mono.error(new IllegalStateException("Unexpected type of ChatFull from storage"));
                        }

                        var updatePts = client.getMtProtoResources()
                                .getStoreLayout().updateChannelPts(channelId, pts);

                        int localPts = f.pts();
                        if (localPts + ptsCount < pts) {
                            log.debug("Updates gap found for channel {}. Received pts: {}-{}, local pts: {}",
                                    channelId, pts - ptsCount, pts, localPts);

                            return client.getMtProtoResources().getStoreLayout().resolveChannel(channelId)
                                    .flatMapMany(c -> getChannelDifference(c, localPts));
                        } else if (localPts + ptsCount > pts) {
                            return Flux.empty();
                        }

                        if (log.isDebugEnabled() && notFromDiff) {
                            log.debug("Updating state for channel: {}, pts: {}->{}", channelId, localPts, pts);
                        }
                        return updatePts.thenMany(mapUpdate);
                    });
        } else if (isQtsUpdate(u)) {
            int newQts;
            // region qts extraction
            switch (u.identifier()) {
                case UpdateNewEncryptedMessage.ID -> {
                    var q = (UpdateNewEncryptedMessage) u;
                    newQts = q.qts();
                }
                case UpdateMessagePollVote.ID -> {
                    var q = (UpdateMessagePollVote) u;
                    newQts = q.qts();
                }
                case UpdateBotStopped.ID -> {
                    var q = (UpdateBotStopped) u;
                    newQts = q.qts();
                }
                case UpdateChatParticipant.ID -> {
                    var q = (UpdateChatParticipant) u;
                    newQts = q.qts();
                }
                case UpdateChannelParticipant.ID -> {
                    var q = (UpdateChannelParticipant) u;
                    newQts = q.qts();
                }
                case UpdateBotChatInviteRequester.ID -> {
                    var q = (UpdateBotChatInviteRequester) u;
                    newQts = q.qts();
                }
                default -> throw new IllegalStateException("Unexpected qts update type: " + u);
            }
            // endregion

            boolean botQtsUpdate = isBotQtsUpdate(u);
            int qts = this.qts;
            if (botQtsUpdate && qts == 0) {
                if (log.isDebugEnabled() && notFromDiff) {
                    log.debug("Updating state, qts: {}->{}", qts, newQts);
                }

                this.qts = newQts;

                return saveStateIf(true)
                        .thenMany(mapUpdate);
            } else {
                if (qts + 1 < newQts) {
                    log.debug("Updates gap found. Received qts: {}, local qts: {}", newQts, qts);

                    return getDifference(pts, qts, date);
                } else if (qts + 1 > newQts) {
                    return Flux.empty();
                } else {
                    if (log.isDebugEnabled() && notFromDiff) {
                        log.debug("Updating state, qts: {}->{}", qts, newQts);
                    }

                    this.qts = newQts;

                    return saveStateIf(true)
                            .thenMany(mapUpdate);
                }
            }
        } else {
            return mapUpdate;
        }
    }

    protected Flux<Event> getChannelDifference(InputChannel id, int pts) {
        int limit = options.channelDifferenceLimit;

        if (log.isDebugEnabled()) {
            log.debug("Getting channel difference, channel: {}, pts: {}, limit: {}", getRawPeerId(id), pts, limit);
        }

        var request = GetChannelDifference.builder()
                .force(false)
                .channel(id)
                .pts(pts)
                .filter(ChannelMessagesFilterEmpty.instance())
                .limit(limit)
                .build();

        return client.getMtProtoClientGroup()
                .send(DcId.main(), request)
                .flatMapMany(diff -> handleChannelDifference(request, diff))
                .onErrorResume(RpcException.isErrorMessage("CHANNEL_PRIVATE"), e -> Mono.empty());
    }

    @Nullable
    protected Chat getChatEntity(Id peer, Map<Id, Chat> chatsMap, Map<Id, User> usersMap, @Nullable User selfUser) {
        return switch (peer.getType()) {
            case CHAT, CHANNEL -> chatsMap.get(peer);
            case USER -> {
                User data = usersMap.get(peer);
                yield data != null ? new PrivateChat(client, data, selfUser) : null;
            }
        };
    }

    /**
     * @param checkin Interval for retrieving {@link GetState}.
     * @param channelDifferenceLimit Maximal amount of updates in {@link GetChannelDifference} requests.
     * @param discardMinimalMessageUpdates Whether received {@link UpdateShortChatMessage} and {@link UpdateShortMessage}
     * updates will be ignored and refetched as normal message events.
     */
    // TODO limit for common difference?
    public record Options(Duration checkin, int channelDifferenceLimit, boolean discardMinimalMessageUpdates) {
        public static final int MAX_USER_CHANNEL_DIFFERENCE = 100;
        public static final int MAX_BOT_CHANNEL_DIFFERENCE  = 100000;
        public static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(1);
        public static final boolean DEFAULT_DISCARD_MINIMAL_MESSAGE_UPDATES = false;

        public Options(MTProtoTelegramClient client) {
            this(DEFAULT_CHECKIN, client.getAuthResources().isBot()
                    ? MAX_BOT_CHANNEL_DIFFERENCE
                    : MAX_USER_CHANNEL_DIFFERENCE,
                    DEFAULT_DISCARD_MINIMAL_MESSAGE_UPDATES);
        }

        public Options {
            Objects.requireNonNull(checkin);
            // TODO: other checks
        }
    }
}
