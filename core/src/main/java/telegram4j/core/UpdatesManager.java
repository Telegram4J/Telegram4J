package telegram4j.core;

import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import telegram4j.core.event.dispatcher.UpdateContext;
import telegram4j.core.event.dispatcher.UpdatesMapper;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.Id;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.ResettableInterval;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.ImmutableChatFull;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.ImmutableGetChannelDifference;
import telegram4j.tl.request.updates.ImmutableGetDifference;
import telegram4j.tl.updates.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

/** Manager for correct and complete work with general and channel updates. */
public class UpdatesManager {

    private static final Logger log = Loggers.getLogger(UpdatesManager.class);

    private static final int MAX_CHANNEL_DIFFERENCE = 100;
    private static final int MAX_BOT_CHANNEL_DIFFERENCE = 100000;
    private static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(2);

    private final MTProtoTelegramClient client;
    private final UpdatesMapper updatesMapper;
    private final ResettableInterval stateInterval = new ResettableInterval(Schedulers.parallel());

    private volatile int pts = -1;
    private volatile int qts = -1;
    private volatile int date = -1;
    private volatile int seq = -1;

    public UpdatesManager(MTProtoTelegramClient client, UpdatesMapper updatesMapper) {
        this.client = Objects.requireNonNull(client, "client");
        this.updatesMapper = Objects.requireNonNull(updatesMapper, "updatesMapper");

        stateInterval.start(DEFAULT_CHECKIN, DEFAULT_CHECKIN);
    }

    public Mono<Void> start() {
        Mono<Void> checkinInterval = stateInterval.ticks()
                .flatMap(t -> fillGap())
                .then();

        return Mono.when(checkinInterval);
    }

    public Mono<Void> fillGap() {
        return client.getServiceHolder()
        .getUpdatesService().getState()
        .flatMap(state -> {
            if (date == -1 || pts == -1 || seq == -1 || qts == -1) {
                return client.getMtProtoResources().getStoreLayout()
                        .getCurrentState()
                        .filter(s -> s.qts() != -1 && s.seq() != -1
                                && s.pts() != -1 && s.date() != -1)
                        .defaultIfEmpty(state)
                        .doOnNext(this::applyStateLocal);
            }
            return Mono.empty();
        })
        .thenMany(getDifference())
        .doOnNext(client.getMtProtoResources()
                .getEventDispatcher()::publish)
        .then();
    }

    public Flux<Event> handle(Updates updates) {
        switch (updates.identifier()) {
            case UpdatesTooLong.ID:
                return getDifference();
            case UpdateShort.ID: {
                UpdateShort data = (UpdateShort) updates;

                date = data.date();

                return updatesMapper.handle(UpdateContext.create(client, data.update()));
            }
            case BaseUpdates.ID: {
                BaseUpdates data = (BaseUpdates) updates;

                Flux<Event> preApply = Flux.empty();
                int updSeq = data.seq();
                if (updSeq != 0 && seq + 1 < updSeq) {
                    log.debug("Updates seq gap found. Received seq: {}-{}, local seq: {}", updSeq, updSeq + 1, seq);
                    preApply = getDifference();
                } else if (updSeq != 0 && seq + 1 > updSeq) {
                    return Flux.empty();
                }

                var usersMap = data.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(u -> (BaseUser) u)
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));
                var chatsMap = data.chats().stream()
                        .filter(TlEntityUtil::isAvailableChat)
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                if (updSeq != 0) {
                    seq = updSeq;
                }

                date = data.date();

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(chatsMap.values(), usersMap.values());

                Flux<Event> events = Flux.fromIterable(data.updates())
                        .flatMap(update -> updatesMapper.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)));

                return saveContacts.thenMany(preApply.concatWith(events));
            }
            case UpdatesCombined.ID: {
                UpdatesCombined data = (UpdatesCombined) updates;

                Flux<Event> preApply = Flux.empty();
                int seqBegin = data.seqStart();
                if (seqBegin != 0 && seq + 1 < seqBegin) {
                    log.debug("Updates seq gap found. Received seq: {}-{}, local seq: {}", seqBegin, data.seq(), seq);
                    preApply = getDifference();
                } else if (seqBegin != 0 && seq + 1 > seqBegin) {
                    return Flux.empty();
                }

                var usersMap = data.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(u -> (BaseUser) u)
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));
                var chatsMap = data.chats().stream()
                        .filter(TlEntityUtil::isAvailableChat)
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                if (data.seq() != 0) {
                    seq = data.seq();
                }

                date = data.date();

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(chatsMap.values(), usersMap.values());

                Flux<Event> events = Flux.fromIterable(data.updates())
                        .flatMap(update -> updatesMapper.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)));

                return saveContacts.thenMany(preApply.concatWith(events));
            }
            case UpdateShortChatMessage.ID: {
                UpdateShortChatMessage data = (UpdateShortChatMessage) updates;

                Flux<Event> preApply = Flux.empty();
                if (pts + data.ptsCount() < data.pts()) {
                    log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                            data.pts(), data.pts() + data.ptsCount(), pts);
                    preApply = getDifference();
                } else if (pts + data.ptsCount() > data.pts()) {
                    return Flux.empty();
                }

                pts = data.pts();

                var message = BaseMessage.builder()
                        .out(data.out())
                        .mentioned(data.mentioned())
                        .mediaUnread(data.mediaUnread())
                        .silent(data.silent())
                        .id(data.id())
                        .date(data.date())
                        .fromId(ImmutablePeerUser.of(data.fromId()))
                        .peerId(ImmutablePeerChat.of(data.chatId()))
                        .fwdFrom(data.fwdFrom())
                        .viaBotId(data.viaBotId())
                        .replyTo(data.replyTo())
                        .entities(data.entities())
                        .ttlPeriod(data.ttlPeriod())
                        .message(data.message());

                Flux<Event> mapUpdate = updatesMapper.handle(UpdateContext.create(client, UpdateNewMessage.builder()
                        .message(message.build())
                        .pts(data.pts())
                        .ptsCount(data.ptsCount())
                        .build()));

                return preApply.concatWith(mapUpdate);
            }
            case UpdateShortMessage.ID: {
                UpdateShortMessage data = (UpdateShortMessage) updates;

                Flux<Event> preApply = Flux.empty();
                if (pts + data.ptsCount() < data.pts()) {
                    log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                            data.pts(), data.pts() + data.ptsCount(), pts);
                    preApply = getDifference();
                } else if (pts + data.ptsCount() > data.pts()) {
                    return Flux.empty();
                }

                pts = data.pts();

                var message = BaseMessage.builder()
                        .out(data.out())
                        .mentioned(data.mentioned())
                        .mediaUnread(data.mediaUnread())
                        .silent(data.silent())
                        .id(data.id())
                        .date(data.date())
                        .fwdFrom(data.fwdFrom())
                        .viaBotId(data.viaBotId())
                        .replyTo(data.replyTo())
                        .entities(data.entities())
                        .ttlPeriod(data.ttlPeriod())
                        .message(data.message());

                if (data.out()) {
                    message.peerId(ImmutablePeerUser.of(data.userId()));
                    message.fromId(ImmutablePeerUser.of(client.getSelfId().asLong()));
                } else {
                    message.peerId(ImmutablePeerUser.of(client.getSelfId().asLong()));
                    message.fromId(ImmutablePeerUser.of(data.userId()));
                }

                Flux<Event> mapUpdate = updatesMapper.handle(UpdateContext.create(client, UpdateNewMessage.builder()
                        .message(message.build())
                        .pts(data.pts())
                        .ptsCount(data.ptsCount())
                        .build()));

                return preApply.concatWith(mapUpdate);
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown updates type: " + updates));
        }
    }

    // private methods
    // ================

    private void applyStateLocal(State state) {
        pts = state.pts();
        qts = state.qts();
        date = state.date();
        seq = state.seq();
    }

    private Mono<Void> applyState(State state) {
        return Mono.defer(() -> {
            if (log.isDebugEnabled()) {
                StringJoiner j = new StringJoiner(", ");
                if (this.pts != state.pts()) {
                    j.add("pts: " + this.pts + "->" + state.pts());
                }
                if (this.qts != state.qts()) {
                    j.add("qts: " + this.qts + "->" + state.qts());
                }
                if (this.seq != state.seq()) {
                    j.add("seq: " + seq + "->" + state.seq());
                }
                if (this.date != state.date()) {
                    j.add("date: " + Instant.ofEpochSecond(this.date) + "->" + Instant.ofEpochSecond(state.date()));
                }

                String str = j.toString();
                if (str.isEmpty()) {
                    return Mono.empty();
                }

                log.debug("Updating state, " + j);
            }

            applyStateLocal(state);
            return client.getMtProtoResources()
                    .getStoreLayout()
                    .updateState(state);
        });
    }

    private Flux<Event> getDifference() {
        return Flux.defer(() -> getDifference(pts, qts, date));
    }

    private Flux<Event> getDifference(int pts, int qts, int date) {
        if (pts == -1 || qts == -1 || date == -1) {
            log.debug("Incorrect get difference parameters, pts: {}, qts: {}, date: {}", pts, qts, Instant.ofEpochSecond(date));
            return Flux.empty();
        }

        // It often turns out that getDifference() returns the updates already received,
        // but for some unknown reason sets them on a different date, which is longer by DEFAULT_CHECKIN interval
        if (client.isBot()) {
            date += DEFAULT_CHECKIN.getSeconds();
        }

        if (log.isDebugEnabled()) {
            log.debug("Getting difference, pts: {}, qts: {}, date: {}", pts, qts, Instant.ofEpochSecond(date));
        }

        stateInterval.start(DEFAULT_CHECKIN, DEFAULT_CHECKIN);
        return client.getServiceHolder()
                .getUpdatesService()
                .getDifference(ImmutableGetDifference.of(pts, date, qts))
                .flatMapMany(difference -> {
                    if (log.isTraceEnabled()) {
                        log.trace("difference: {}", difference);
                    }

                    switch (difference.identifier()) {
                        case DifferenceEmpty.ID: {
                            DifferenceEmpty data = (DifferenceEmpty) difference;

                            this.seq = data.seq();
                            this.date = data.date();
                            return Mono.empty();
                        }
                        case BaseDifference.ID: {
                            BaseDifference difference0 = (BaseDifference) difference;

                            // currently, ignored
                            // difference0.newEncryptedMessages()

                            var chatsMap = difference0.chats().stream()
                                    .filter(TlEntityUtil::isAvailableChat)
                                    .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                            var usersMap = difference0.users().stream()
                                    .filter(u -> u.identifier() == BaseUser.ID)
                                    .map(u -> (BaseUser) u)
                                    .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));

                            var selfUser = usersMap.values().stream()
                                    .filter(BaseUser::self)
                                    .findFirst()
                                    .orElse(null);

                            Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(difference0.newMessages())
                                    .ofType(BaseMessageFields.class)
                                    .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                            .getStoreLayout().existMessage(message)))
                                    .flatMap(data -> {
                                        var chat = mapChat(data.peerId(), chatsMap, usersMap, selfUser)
                                                .orElse(null);

                                        var author = Optional.ofNullable(data.fromId())
                                                .flatMap(p -> mapPeer(p, chatsMap, usersMap))
                                                .orElse(null);

                                        return Mono.justOrEmpty(Optional.ofNullable(chat).map(Chat::getId))
                                                .switchIfEmpty(client.asInputPeer(Id.of(data.peerId())).map(p -> Id.of(p, client.getSelfId())))
                                                .map(i -> EntityFactory.createMessage(client, data, i))
                                                .map(m -> new SendMessageEvent(client, m, chat, author));
                                    });

                            Flux<Event> applyChannelDifference = Mono.justOrEmpty(difference0.otherUpdates().stream()
                                            .filter(u -> u.identifier() == UpdateChannelTooLong.ID)
                                            .map(u -> (UpdateChannelTooLong) u)
                                            .findFirst())
                                    .zipWhen(u -> client.getMtProtoResources()
                                            .getStoreLayout()
                                            .getChatFullById(u.channelId())
                                            .switchIfEmpty(client.getMtProtoResources().getStoreLayout()
                                                    .resolveChannel(u.channelId())
                                                    .flatMap(c -> client.getServiceHolder()
                                                            .getChatService()
                                                            .getFullChannel(c)))
                                            .map(ChatFull::fullChat)
                                            .ofType(ChannelFull.class)
                                            .map(ChannelFull::pts)
                                            .defaultIfEmpty(-1))
                                    .filter(TupleUtils.predicate((u, c) -> c == -1 || u.pts() == null ||
                                            Objects.requireNonNull(u.pts()) > c))
                                    .flatMapMany(TupleUtils.function((u, cpts) -> {
                                        var channel = Optional.ofNullable(chatsMap.get(u.channelId()))
                                                .filter(ch -> ch.identifier() == Channel.ID)
                                                .map(ch -> (Channel) ch)
                                                .orElseThrow();

                                        InputChannel id = ImmutableBaseInputChannel.of(channel.id(),
                                                Objects.requireNonNull(channel.accessHash()));

                                        Integer upts = u.pts();
                                        int dpts = Math.max(1, Math.max(upts != null ? upts : -1, cpts));
                                        int limit = client.isBot()
                                                ? MAX_BOT_CHANNEL_DIFFERENCE
                                                : MAX_CHANNEL_DIFFERENCE;

                                        if (log.isDebugEnabled()) {
                                            log.debug("Getting channel difference, channel: {}, pts: {}, limit: {}", channel.id(), dpts, limit);
                                        }

                                        var request = GetChannelDifference.builder()
                                                .force(true)
                                                .channel(id)
                                                .pts(dpts)
                                                .filter(ChannelMessagesFilterEmpty.instance())
                                                .limit(limit)
                                                .build();

                                        return client.getServiceHolder()
                                                .getUpdatesService()
                                                .getChannelDifference(request)
                                                .flatMapMany(diff -> handleChannelDifference(request, diff));
                                    }));

                            Flux<Event> concatedUpdates = Flux.fromIterable(difference0.otherUpdates())
                                    .flatMap(update -> updatesMapper.handle(UpdateContext.create(
                                            client, chatsMap, usersMap, update)))
                                    .concatWith(messageCreateEvents)
                                    .concatWith(applyChannelDifference);

                            Mono<Void> saveContacts = client.getMtProtoResources()
                                    .getStoreLayout().onContacts(chatsMap.values(), usersMap.values());

                            return saveContacts.and(applyState(difference0.state()))
                                    .thenMany(concatedUpdates);
                        }
                        case DifferenceSlice.ID: {
                            DifferenceSlice difference0 = (DifferenceSlice) difference;

                            var chatsMap = difference0.chats().stream()
                                    .filter(TlEntityUtil::isAvailableChat)
                                    .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                            var usersMap = difference0.users().stream()
                                    .filter(u -> u.identifier() == BaseUser.ID)
                                    .map(u -> (BaseUser) u)
                                    .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));

                            var selfUser = usersMap.values().stream()
                                    .filter(BaseUser::self)
                                    .findFirst()
                                    .orElse(null);

                            Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(difference0.newMessages())
                                    .ofType(BaseMessageFields.class)
                                    .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                            .getStoreLayout().existMessage(message)))
                                    .flatMap(data -> {
                                        var chat = mapChat(data.peerId(), chatsMap, usersMap, selfUser)
                                                .orElse(null);

                                        var author = Optional.ofNullable(data.fromId())
                                                .flatMap(p -> mapPeer(p, chatsMap, usersMap))
                                                .orElse(null);

                                        return Mono.justOrEmpty(Optional.ofNullable(chat).map(Chat::getId))
                                                .switchIfEmpty(client.asInputPeer(Id.of(data.peerId())).map(p -> Id.of(p, client.getSelfId())))
                                                .map(i -> EntityFactory.createMessage(client, data, i))
                                                .map(m -> new SendMessageEvent(client, m, chat, author));
                                    });

                            Mono<Void> saveContacts = client.getMtProtoResources()
                                    .getStoreLayout().onContacts(chatsMap.values(), usersMap.values());

                            applyStateLocal(difference0.intermediateState());

                            return saveContacts.thenMany(Flux.fromIterable(difference0.otherUpdates()))
                                    .flatMap(update -> updatesMapper.handle(UpdateContext.create(
                                            client, chatsMap, usersMap, update)))
                                    .concatWith(messageCreateEvents)
                                    .transform(f -> f.concatWith(getDifference()));
                        }
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown difference type: " + difference));
                    }
                });
    }

    private Flux<Event> handleChannelDifference(GetChannelDifference request, ChannelDifference diff) {
        if (log.isTraceEnabled()) {
            log.trace("channel difference: {}", diff);
        }

        int newPts;
        switch (diff.identifier()) {
            case BaseChannelDifference.ID:
                newPts = ((BaseChannelDifference) diff).pts();
                break;
            case ChannelDifferenceEmpty.ID:
                newPts = ((ChannelDifferenceEmpty) diff).pts();
                break;
            case ChannelDifferenceTooLong.ID:
                ChannelDifferenceTooLong diff0 = (ChannelDifferenceTooLong) diff;
                if (diff0.dialog().identifier() == BaseDialog.ID) {
                    BaseDialog dialog = (BaseDialog) diff0.dialog();
                    Integer pts = dialog.pts();
                    newPts = pts != null ? pts : 1;
                } else {
                    newPts = 1;
                }
                break;
            default: throw new IllegalArgumentException("Unknown channel difference type: " + diff);
        }

        long channelId = ((BaseInputChannel) request.channel()).channelId();
        Mono<Void> updatePts = client.getMtProtoResources()
                .getStoreLayout().getChatFullById(channelId)
                .map(c -> ImmutableChatFull.copyOf(c)
                        .withFullChat(ImmutableChannelFull.copyOf((ChannelFull) c.fullChat())
                                .withPts(newPts)))
                .flatMap(c -> client.getMtProtoResources()
                        .getStoreLayout().onChatUpdate(c))
                .then();

        Flux<Event> refetchDifference = Flux.defer(() -> {
            if (diff.finalState()) {
                return Flux.empty();
            }

            if (log.isDebugEnabled()) {
                log.debug("Getting non-final channel difference, channel: {}, pts: {}, limit: {}",
                        channelId, newPts, request.limit());
            }

            var updRequest = ImmutableGetChannelDifference.copyOf(request)
                    .withPts(newPts);

            return client.getServiceHolder()
                    .getUpdatesService()
                    .getChannelDifference(updRequest)
                    .flatMapMany(d -> handleChannelDifference(updRequest, d));
        });

        switch (diff.identifier()) {
            case BaseChannelDifference.ID: {
                BaseChannelDifference diff0 = (BaseChannelDifference) diff;

                var chatsMap = diff0.chats().stream()
                        .filter(TlEntityUtil::isAvailableChat)
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));
                var usersMap = diff0.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(u -> (BaseUser) u)
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));

                var selfUser = usersMap.values().stream()
                        .filter(BaseUser::self)
                        .findFirst()
                        .orElse(null);

                Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(diff0.newMessages())
                        .ofType(BaseMessageFields.class)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message)))
                        .flatMap(data -> {
                            var chat = mapChat(data.peerId(), chatsMap, usersMap, selfUser)
                                    .orElse(null);

                            var author = Optional.ofNullable(data.fromId())
                                    .flatMap(p -> mapPeer(p, chatsMap, usersMap))
                                    .orElse(null);

                            return Mono.justOrEmpty(chat)
                                    .map(Chat::getId)
                                    .switchIfEmpty(client.asInputPeer(Id.of(data.peerId())).map(p -> Id.of(p, client.getSelfId())))
                                    .map(i -> EntityFactory.createMessage(client, data, i))
                                    .map(m -> new SendMessageEvent(client, m, chat, author));
                        });

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(chatsMap.values(), usersMap.values());

                return saveContacts.and(updatePts)
                        .thenMany(Flux.fromIterable(diff0.otherUpdates()))
                        .flatMap(update -> updatesMapper.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)))
                        .concatWith(messageCreateEvents)
                        .concatWith(refetchDifference);
            }
            case ChannelDifferenceEmpty.ID: {
                ChannelDifferenceEmpty diff0 = (ChannelDifferenceEmpty) diff;

                return updatePts.thenMany(Flux.empty());
            }
            case ChannelDifferenceTooLong.ID: {
                ChannelDifferenceTooLong diff0 = (ChannelDifferenceTooLong) diff;

                var chatsMap = diff0.chats().stream()
                        .filter(TlEntityUtil::isAvailableChat)
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));
                var usersMap = diff0.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(u -> (BaseUser) u)
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));

                var selfUser = usersMap.values().stream()
                        .filter(BaseUser::self)
                        .findFirst()
                        .orElse(null);

                Flux<Event> messageCreateEvents = Flux.fromIterable(diff0.messages())
                        .ofType(BaseMessageFields.class)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message)))
                        .flatMap(data -> {
                            var chat = mapChat(data.peerId(), chatsMap, usersMap, selfUser)
                                .orElse(null);

                            var author = Optional.ofNullable(data.fromId())
                                    .flatMap(p -> mapPeer(p, chatsMap, usersMap))
                                    .orElse(null);

                            return Mono.justOrEmpty(chat)
                                    .map(Chat::getId)
                                    .switchIfEmpty(client.asInputPeer(Id.of(data.peerId())).map(p -> Id.of(p, client.getSelfId())))
                                    .map(i -> EntityFactory.createMessage(client, data, i))
                                    .map(m -> new SendMessageEvent(client, m, chat, author));
                        });

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(chatsMap.values(), usersMap.values());

                return updatePts.and(saveContacts)
                        .thenMany(messageCreateEvents)
                        .concatWith(refetchDifference);
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown channel difference type: " + diff));
        }
    }

    private Optional<Chat> mapChat(Peer peer, Map<Long, telegram4j.tl.Chat> chatsMap,
                                   Map<Long, BaseUser> usersMap, @Nullable BaseUser selfUser) {
        long rawId = getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerUser.ID:
                return Optional.ofNullable(usersMap.get(rawId))
                        .map(u -> EntityFactory.createChat(client, u, selfUser));
            case PeerChat.ID:
            case PeerChannel.ID:
                return Optional.ofNullable(chatsMap.get(rawId))
                        .map(c -> EntityFactory.createChat(client, c, null));
            default:
                throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    private Optional<PeerEntity> mapPeer(Peer peer, Map<Long, telegram4j.tl.Chat> chatsMap,
                                         Map<Long, BaseUser> usersMap) {
        long rawId = getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerUser.ID:
                return Optional.ofNullable(usersMap.get(rawId))
                        .map(u -> EntityFactory.createUser(client, u));
            case PeerChat.ID:
            case PeerChannel.ID:
                return Optional.ofNullable(chatsMap.get(rawId))
                        .map(c -> EntityFactory.createChat(client, c, null));
            default:
                throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    public void shutdown() {
        stateInterval.dispose();
    }
}
