package telegram4j.core.event;

import reactor.bool.BooleanUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.Id;
import telegram4j.mtproto.util.ResettableInterval;
import telegram4j.tl.*;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.ImmutableChatFull;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.ImmutableGetChannelDifference;
import telegram4j.tl.request.updates.ImmutableGetDifference;
import telegram4j.tl.updates.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

/** Manager for correct and complete work with general and channel updates. */
public class DefaultUpdatesManager implements UpdatesManager {

    private static final Logger log = Loggers.getLogger(DefaultUpdatesManager.class);

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

    public DefaultUpdatesManager(MTProtoTelegramClient client, UpdatesMapper updatesMapper) {
        this.client = Objects.requireNonNull(client);
        this.updatesMapper = Objects.requireNonNull(updatesMapper);
    }

    @Override
    public Mono<Void> start() {
        Mono<Void> checkinInterval = stateInterval.ticks()
                .flatMap(t -> fillGap())
                .then();

        return Mono.when(checkinInterval);
    }

    @Override
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

    @Override
    public Flux<Event> handle(Updates updates) {
        switch (updates.identifier()) {
            case UpdatesTooLong.ID:
                return getDifference();
            case UpdateShort.ID: {
                UpdateShort data = (UpdateShort) updates;

                date = data.date();

                return applyUpdate(UpdateContext.create(client, data.update()));
            }
            case BaseUpdates.ID: {
                BaseUpdates data = (BaseUpdates) updates;

                Flux<Event> preApply = Flux.empty();
                int updSeq = data.seq();
                int seq = this.seq;
                if (updSeq != 0 && seq + 1 < updSeq) {
                    log.debug("Updates seq gap found. Received seq: {}-{}, local seq: {}", updSeq, updSeq + 1, seq);
                    preApply = getDifference();
                } else if (updSeq != 0 && seq + 1 > updSeq) {
                    return Flux.empty();
                }

                if (updSeq != 0) {
                    this.seq = updSeq;
                }

                date = data.date();

                return preApply.concatWith(handleUpdates0(List.of(), data.updates(),
                        data.chats(), data.users()));
            }
            case UpdatesCombined.ID: {
                UpdatesCombined data = (UpdatesCombined) updates;

                Flux<Event> preApply = Flux.empty();
                int seqBegin = data.seqStart();
                int seq = this.seq;
                if (seqBegin != 0 && seq + 1 < seqBegin) {
                    log.debug("Updates seq gap found. Received seq: {}-{}, local seq: {}", seqBegin, data.seq(), seq);

                    preApply = getDifference();
                } else if (seqBegin != 0 && seq + 1 > seqBegin) {
                    return Flux.empty();
                }

                if (data.seq() != 0) {
                    this.seq = data.seq();
                }

                date = data.date();

                return preApply.concatWith(handleUpdates0(List.of(), data.updates(),
                        data.chats(), data.users()));
            }
            case UpdateShortChatMessage.ID: {
                UpdateShortChatMessage data = (UpdateShortChatMessage) updates;

                Flux<Event> preApply = Flux.empty();
                int pts = this.pts;
                if (pts + data.ptsCount() < data.pts()) {
                    log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                            data.pts() - data.ptsCount(), data.pts(), pts);
                    preApply = getDifference();
                } else if (pts + data.ptsCount() > data.pts()) {
                    return Flux.empty();
                }

                this.pts = data.pts();

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
                int pts = this.pts;
                if (pts + data.ptsCount() < data.pts()) {
                    log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                            data.pts() - data.ptsCount(), data.pts(), pts);
                    preApply = getDifference();
                } else if (pts + data.ptsCount() > data.pts()) {
                    return Flux.empty();
                }

                this.pts = data.pts();

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
                return Flux.error(new IllegalArgumentException("Unknown Updates type: " + updates));
        }
    }

    @Override
    public void shutdown() {
        stateInterval.dispose();
    }

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

                            return applyState(difference0.state())
                                    .thenMany(handleUpdates0(difference0.newMessages(), difference0.otherUpdates(),
                                            difference0.chats(), difference0.users()));
                        }
                        case DifferenceSlice.ID: {
                            DifferenceSlice difference0 = (DifferenceSlice) difference;

                            applyStateLocal(difference0.intermediateState());

                            return handleUpdates0(difference0.newMessages(), difference0.otherUpdates(),
                                    difference0.chats(), difference0.users())
                                    .concatWith(getDifference());
                        }
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown difference type: " + difference));
                    }
                });
    }

    private Flux<Event> handleUpdates0(List<Message> newMessages, List<Update> otherUpdates,
                                       List<telegram4j.tl.Chat> chats, List<telegram4j.tl.User> users) {
        var usersMap = users.stream()
                .map(u -> EntityFactory.createUser(client, u))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));

        var selfUser = usersMap.get(client.getSelfId().asLong());

        var chatsMap = chats.stream()
                .map(u -> EntityFactory.createChat(client, u, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));

        Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(newMessages)
                .ofType(BaseMessageFields.class)
                .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                        .getStoreLayout().existMessage(message)))
                .flatMap(data -> {
                    var chat = getChatEntity(data.peerId(), chatsMap, usersMap, selfUser);

                    var author = Optional.ofNullable(data.fromId())
                            .flatMap(p -> getPeerEntity(p, chatsMap, usersMap))
                            .or(() -> Optional.ofNullable(chat)
                                    .filter(c -> c.getType() == Chat.Type.PRIVATE)
                                    .map(c -> ((PrivateChat) c).getUser()))
                            .orElse(null);

                    return Mono.justOrEmpty(chat)
                            .map(Chat::getId)
                            .switchIfEmpty(client.asInputPeer(Id.of(data.peerId()))
                                    .map(p -> Id.of(p, client.getSelfId())))
                            .map(i -> EntityFactory.createMessage(client, data, i))
                            .map(m -> new SendMessageEvent(client, m, chat, author));
                });

        Flux<Event> applyChannelDifference = Flux.fromIterable(otherUpdates)
                .ofType(UpdateChannelTooLong.class)
                .flatMap(u -> client.getMtProtoResources().getStoreLayout()
                        .getChannelFullById(u.channelId())
                        .switchIfEmpty(client.getMtProtoResources().getStoreLayout()
                                .resolveChannel(u.channelId())
                                .flatMap(client.getServiceHolder().getChatService()::getFullChannel)
                                .then(Mono.empty())) // no channel pts; can't request channel updates
                        .map(ChatFull::fullChat)
                        .cast(ChannelFull.class)
                        .map(ChannelFull::pts)
                        .map(i -> Tuples.of(u, i)))
                .filter(TupleUtils.predicate((u, c) -> Optional.ofNullable(u.pts()).map(i -> i > c).orElse(true)))
                .flatMap(TupleUtils.function((u, cpts) -> {
                    var id = Optional.ofNullable(chatsMap.get(u.channelId()))
                            .map(ch -> (Channel) ch)
                            .map(ch -> ImmutableBaseInputChannel.of(ch.id(),
                                    Objects.requireNonNull(ch.accessHash())))
                            .orElseThrow();

                    Integer upts = (upts = u.pts()) == null ? -1 : upts;
                    int dpts = Math.max(1, Math.max(upts, cpts));
                    int limit = client.getAuthResources().isBot()
                            ? MAX_BOT_CHANNEL_DIFFERENCE
                            : MAX_CHANNEL_DIFFERENCE;

                    if (log.isDebugEnabled()) {
                        log.debug("Getting channel difference, channel: {}, pts: {}, limit: {}", u.channelId(), dpts, limit);
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

        Flux<Event> concatedUpdates = Flux.fromIterable(otherUpdates)
                .map(u -> UpdateContext.create(client, chatsMap, usersMap, u))
                .flatMap(this::applyUpdate)
                .concatWith(messageCreateEvents)
                .concatWith(applyChannelDifference);

        return client.getMtProtoResources()
                .getStoreLayout().onContacts(chats, users)
                .thenMany(concatedUpdates);
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

        Id channelId = Id.of(request.channel(), client.getSelfId());
        Mono<Void> updatePts = client.getMtProtoResources()
                .getStoreLayout().getChannelFullById(channelId.asLong())
                .map(c -> ImmutableChatFull.copyOf(c)
                        .withFullChat(ImmutableChannelFull.copyOf((ChannelFull) c.fullChat())
                                .withPts(newPts)))
                .flatMap(c -> client.getMtProtoResources()
                        .getStoreLayout().onChatUpdate(c))
                .then();

        Flux<Event> refetchDifference = Flux.defer(() -> {
            if (diff.isFinal()) {
                return Flux.empty();
            }

            if (log.isDebugEnabled()) {
                log.debug("Getting non-final channel difference, channel: {}, pts: {}, limit: {}",
                        channelId.asLong(), newPts, request.limit());
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

                var usersMap = diff0.users().stream()
                        .map(u -> EntityFactory.createUser(client, u))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));

                var selfUser = usersMap.get(client.getSelfId().asLong());

                var chatsMap = diff0.chats().stream()
                        .map(u -> EntityFactory.createChat(client, u, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));

                Flux<Event> messageCreateEvents = Flux.fromIterable(diff0.newMessages())
                        .ofType(BaseMessageFields.class)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message)))
                        .map(data -> {
                            var chat = getChatEntity(data.peerId(), chatsMap, usersMap, selfUser);

                            var author = Optional.ofNullable(data.fromId())
                                    .flatMap(p -> getPeerEntity(p, chatsMap, usersMap))
                                    .orElse(null);

                            var msg = EntityFactory.createMessage(client, data, channelId);

                            return new SendMessageEvent(client, msg, chat, author);
                        });

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(diff0.chats(), diff0.users());

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

                var usersMap = diff0.users().stream()
                        .map(u -> EntityFactory.createUser(client, u))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));

                var selfUser = usersMap.get(client.getSelfId().asLong());

                var chatsMap = diff0.chats().stream()
                        .map(u -> EntityFactory.createChat(client, u, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));

                Flux<Event> messageCreateEvents = Flux.fromIterable(diff0.messages())
                        .ofType(BaseMessageFields.class)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message)))
                        .map(data -> {
                            var chat = getChatEntity(data.peerId(), chatsMap, usersMap, selfUser);

                            var author = Optional.ofNullable(data.fromId())
                                    .flatMap(p -> getPeerEntity(p, chatsMap, usersMap))
                                    .orElse(null);

                            var msg = EntityFactory.createMessage(client, data, channelId);

                            return new SendMessageEvent(client, msg, chat, author);
                        });

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(diff0.chats(), diff0.users());

                return updatePts.and(saveContacts)
                        .thenMany(messageCreateEvents)
                        .concatWith(refetchDifference);
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown channel difference type: " + diff));
        }
    }

    private Flux<Event> applyUpdate(UpdateContext<Update> ctx) {
        Flux<Event> mapUpdate = updatesMapper.handle(ctx);

        if (ctx.getUpdate() instanceof PtsUpdate) {
            PtsUpdate ptsUpdate = (PtsUpdate) ctx.getUpdate();

            long channelId = -1;
            switch (ptsUpdate.identifier()) {
                case UpdateDeleteChannelMessages.ID: {
                    UpdateDeleteChannelMessages u = (UpdateDeleteChannelMessages) ptsUpdate;

                    channelId = u.channelId();
                    break;
                }
                case UpdateEditChannelMessage.ID: {
                    UpdateEditChannelMessage u = (UpdateEditChannelMessage) ptsUpdate;

                    PeerChannel p = (PeerChannel) ((BaseMessageFields) u.message()).peerId();
                    channelId = getRawPeerId(p);
                    break;
                }
                case UpdateNewChannelMessage.ID: {
                    UpdateNewChannelMessage u = (UpdateNewChannelMessage) ptsUpdate;
                    PeerChannel p = (PeerChannel) ((BaseMessageFields) u.message()).peerId();
                    channelId = getRawPeerId(p);
                    break;
                }
                case UpdatePinnedChannelMessages.ID: {
                    UpdatePinnedChannelMessages u = (UpdatePinnedChannelMessages) ptsUpdate;
                    channelId = u.channelId();
                    break;
                }
                case UpdateReadHistoryOutbox.ID: {
                    UpdateReadHistoryOutbox u = (UpdateReadHistoryOutbox) ptsUpdate;
                    if (u.peer().identifier() == PeerChannel.ID) {
                        channelId = getRawPeerId(u.peer());
                    }
                    break;
                }
                case UpdateChannelWebPage.ID: {
                    UpdateChannelWebPage u = (UpdateChannelWebPage) ptsUpdate;

                    channelId = u.channelId();
                    break;
                }
            }

            Flux<Event> preApply = Flux.empty();
            if (channelId == -1) { // common pts
                int pts = this.pts;
                if (pts + ptsUpdate.ptsCount() < ptsUpdate.pts()) {
                    log.debug("Updates gap found. Received pts: {}-{}, local pts: {}",
                            ptsUpdate.pts() - ptsUpdate.ptsCount(), ptsUpdate.pts(), pts);

                    preApply = getDifference();
                } else if (pts + ptsUpdate.ptsCount() > ptsUpdate.pts()) {
                    return Flux.empty();
                } else {
                    this.pts = ptsUpdate.pts();
                }

                return preApply.concatWith(mapUpdate);
            }

            // channel pts
            long id = channelId;

            // If -1, just apply update
            AtomicBoolean justApplied = new AtomicBoolean();
            return client.getMtProtoResources()
                    .getStoreLayout().getChannelFullById(id)
                    .switchIfEmpty(client.getMtProtoResources()
                            .getStoreLayout().resolveChannel(id)
                            .flatMap(client.getServiceHolder().getChatService()::getFullChannel)
                            .doOnNext(c -> justApplied.set(true)))
                    .map(ChatFull::fullChat)
                    .cast(ChannelFull.class)
                    .map(ChannelFull::pts)
                    .flatMapMany(pts -> {
                        if (justApplied.get()) {
                            return mapUpdate;
                        }

                        if (pts + ptsUpdate.ptsCount() < ptsUpdate.pts()) {
                            log.debug("Updates gap found for channel {}. Received pts: {}-{}, local pts: {}",
                                    id, ptsUpdate.pts() - ptsUpdate.ptsCount(), ptsUpdate.pts(), pts);

                            return client.getMtProtoResources()
                                    .getStoreLayout()
                                    .resolveChannel(id)
                                    .flatMapMany(c -> getChannelDifference(c, pts))
                                    .concatWith(mapUpdate);
                        } else if (pts + ptsUpdate.ptsCount() > ptsUpdate.pts()) {
                            return Flux.empty();
                        }
                        return mapUpdate;
                    });
        }

        if (ctx.getUpdate() instanceof QtsUpdate) {
            QtsUpdate qtsUpdate = (QtsUpdate) ctx.getUpdate();

            Flux<Event> preApply = Flux.empty();
            int qts = this.qts;
            if (qts + 1 < qtsUpdate.qts()) {
                log.debug("Updates gap found. Received qts: {}, local qts: {}", qtsUpdate.qts(), qts);

                preApply = getDifference();
            } else if (qts + 1 > qtsUpdate.qts()) {
                return Flux.empty();
            } else {
                this.qts = qtsUpdate.qts();
            }

            return preApply.concatWith(mapUpdate);
        }

        return mapUpdate;
    }

    private Flux<Event> getChannelDifference(InputChannel id, int pts) {
        int limit = client.getAuthResources().isBot()
                ? MAX_BOT_CHANNEL_DIFFERENCE
                : MAX_CHANNEL_DIFFERENCE;

        if (log.isDebugEnabled()) {
            log.debug("Getting channel difference, channel: {}, pts: {}, limit: {}", getRawPeerId(id), pts, limit);
        }

        var request = GetChannelDifference.builder()
                .force(true)
                .channel(id)
                .pts(pts)
                .filter(ChannelMessagesFilterEmpty.instance())
                .limit(limit)
                .build();

        return client.getServiceHolder()
                .getUpdatesService()
                .getChannelDifference(request)
                .flatMapMany(diff -> handleChannelDifference(request, diff));
    }

    @Nullable
    private Chat getChatEntity(Peer peer, Map<Long, Chat> chatsMap,
                               Map<Long, User> usersMap, @Nullable User selfUser) {
        long rawId = getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerUser.ID:
                User data = usersMap.get(rawId);
                return data != null ? new PrivateChat(client, data, selfUser) : null;
            case PeerChat.ID:
            case PeerChannel.ID: return chatsMap.get(rawId);
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    private Optional<PeerEntity> getPeerEntity(Peer peer, Map<Long, Chat> chatsMap, Map<Long, User> usersMap) {
        long rawId = getRawPeerId(peer);
        switch (peer.identifier()) {
            case PeerUser.ID: return Optional.ofNullable(usersMap.get(rawId));
            case PeerChat.ID:
            case PeerChannel.ID: return Optional.ofNullable(chatsMap.get(rawId));
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }
}
