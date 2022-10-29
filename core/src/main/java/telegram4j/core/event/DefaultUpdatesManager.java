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
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.internal.Preconditions;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.util.ResettableInterval;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.ChatFull;
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

    protected static final Logger log = Loggers.getLogger(DefaultUpdatesManager.class);

    protected final MTProtoTelegramClient client;
    protected final Options options;
    protected final ResettableInterval stateInterval = new ResettableInterval(Schedulers.parallel());

    protected volatile int pts = -1;
    protected volatile int qts = -1;
    protected volatile int date = -1;
    protected volatile int seq = -1;

    public DefaultUpdatesManager(MTProtoTelegramClient client, Options options) {
        this.client = Objects.requireNonNull(client);
        this.options = Objects.requireNonNull(options);
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

                if (log.isDebugEnabled()) {
                    log.debug("Updating state, date: {}->{}", Instant.ofEpochSecond(date), Instant.ofEpochSecond(data.date()));
                }
                date = data.date();

                return applyUpdate(UpdateContext.create(client, data.update()));
            }
            case BaseUpdates.ID: {
                BaseUpdates data = (BaseUpdates) updates;

                Flux<Event> preApply = Flux.empty();
                int seqBegin = data.seq();
                int seqEnd = seqBegin + 1;
                StringJoiner j = new StringJoiner(", ");
                if (seqBegin != 0 && seqEnd != 0) {
                    int seq = this.seq;

                    if (seq + 1 < seqBegin) {
                        log.debug("Updates seq gap found. Received seq: {}-{}, local seq: {}", seqBegin, seqEnd, seq);

                        preApply = getDifference();
                    } else if (seq + 1 > seqBegin) {
                        return Flux.empty();
                    }

                    j.add("seq: " + seq + "->" + seqEnd);
                    this.seq = seqEnd;
                }

                if (log.isDebugEnabled()) {
                    j.add("date: " + Instant.ofEpochSecond(date) + "->" + Instant.ofEpochSecond(data.date()));
                    log.debug("Updating state, " + j);
                }
                date = data.date();

                return preApply.concatWith(handleUpdates0(List.of(), data.updates(),
                        data.chats(), data.users()));
            }
            case UpdatesCombined.ID: {
                UpdatesCombined data = (UpdatesCombined) updates;

                Flux<Event> preApply = Flux.empty();
                int seqBegin = data.seqStart();
                int seqEnd = data.seq();
                Preconditions.requireState(seqEnd - seqBegin == 1);
                StringJoiner j = new StringJoiner(", ");
                if (seqBegin != 0 && seqEnd != 0) {
                    int seq = this.seq;

                    if (seq + 1 < seqBegin) {
                        log.debug("Updates seq gap found. Received seq: {}-{}, local seq: {}", seqBegin, seqEnd, seq);

                        preApply = getDifference();
                    } else if (seq + 1 > seqBegin) {
                        return Flux.empty();
                    }

                    j.add("seq: " + seq + "->" + seqEnd);
                    this.seq = seqEnd;
                }

                if (log.isDebugEnabled()) {
                    j.add("date: " + Instant.ofEpochSecond(date) + "->" + Instant.ofEpochSecond(data.date()));
                    log.debug("Updating state, " + j);
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

                log.debug("Updating state, pts: {}->{}", pts, data.pts());
                this.pts = data.pts();

                var message = BaseMessage.builder()
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
                        .message(data.message());

                Flux<Event> mapUpdate = UpdatesMapper.instance.handle(UpdateContext.create(client, UpdateNewMessage.builder()
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

                log.debug("Updating state, pts: {}->{}", pts, data.pts());
                this.pts = data.pts();

                var message = BaseMessage.builder()
                        .flags(data.flags())
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

                Flux<Event> mapUpdate = UpdatesMapper.instance.handle(UpdateContext.create(client, UpdateNewMessage.builder()
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

    protected void applyStateLocal(State state) {
        pts = state.pts();
        qts = state.qts();
        date = state.date();
        seq = state.seq();
    }

    protected void applyIntermediateState(State state) {
        if (log.isDebugEnabled()) {
            StringJoiner j = new StringJoiner(", ");
            int pts = this.pts;
            if (pts != state.pts()) {
                j.add("pts: " + pts + "->" + state.pts());
                this.pts = state.pts();
            }
            int qts = this.qts;
            if (qts != state.qts()) {
                j.add("qts: " + qts + "->" + state.qts());
                this.qts = state.qts();
            }
            int seq = this.seq;
            if (seq != state.seq()) {
                j.add("seq: " + seq + "->" + state.seq());
                this.seq = state.seq();
            }
            int date = this.date;
            if (date != state.date()) {
                j.add("date: " + Instant.ofEpochSecond(date) + "->" + Instant.ofEpochSecond(state.date()));
                this.date = state.date();
            }

            String str = j.toString();
            if (str.isEmpty()) {
                return;
            }

            log.debug("Updating state to intermediate, " + j);
        } else {
            pts = state.pts();
            qts = state.qts();
            date = state.date();
            seq = state.seq();
        }
    }

    protected Mono<Void> applyState(State state) {
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

    protected Flux<Event> getDifference() {
        return Flux.defer(() -> getDifference(pts, qts, date));
    }

    protected Flux<Event> getDifference(int pts, int qts, int date) {
        if (pts == -1 || qts == -1 || date == -1) {
            if (log.isWarnEnabled()) {
                log.warn("Incorrect get difference parameters, pts: {}, qts: {}, date: {}", pts, qts, Instant.ofEpochSecond(date));
            }
            return Flux.empty();
        }

        if (log.isDebugEnabled()) {
            log.debug("Getting difference, pts: {}, qts: {}, date: {}", pts, qts, Instant.ofEpochSecond(date));
        }

        stateInterval.start(options.checkin, options.checkin);
        return client.getServiceHolder()
                .getUpdatesService()
                .getDifference(ImmutableGetDifference.of(pts, date, qts))
                .flatMapMany(difference -> {
                    if (log.isTraceEnabled()) {
                        log.trace("difference: {}", difference);
                    }

                    switch (difference.identifier()) {
                        case DifferenceEmpty.ID: {
                            DifferenceEmpty diff = (DifferenceEmpty) difference;

                            if (log.isDebugEnabled()) {
                                StringJoiner j = new StringJoiner(", ");
                                int seq = this.seq;
                                if (seq != diff.seq()) {
                                    j.add("seq: " + seq + "->" + diff.seq());
                                }
                                int currDate = this.date;
                                if (currDate != diff.date()) {
                                    j.add("date: " + Instant.ofEpochSecond(currDate) + "->" + Instant.ofEpochSecond(diff.date()));
                                }

                                String str = j.toString();
                                if (str.isEmpty()) {
                                    return Mono.empty();
                                }

                                log.debug("Updating state, " + j);
                            } else {
                                this.seq = diff.seq();
                                this.date = diff.date();
                            }

                            return Mono.empty();
                        }
                        case BaseDifference.ID: {
                            BaseDifference diff = (BaseDifference) difference;

                            return applyState(diff.state())
                                    .thenMany(handleUpdates0(diff.newMessages(), diff.otherUpdates(),
                                            diff.chats(), diff.users()));
                        }
                        case DifferenceSlice.ID: {
                            DifferenceSlice diff = (DifferenceSlice) difference;

                            applyIntermediateState(diff.intermediateState());

                            return handleUpdates0(diff.newMessages(), diff.otherUpdates(), diff.chats(), diff.users())
                                    .concatWith(getDifference());
                        }
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown difference type: " + difference));
                    }
                });
    }

    protected Flux<Event> handleUpdates0(List<Message> newMessages, List<Update> otherUpdates,
                                         List<telegram4j.tl.Chat> chats, List<telegram4j.tl.User> users) {
        var usersMap = users.stream()
                .map(u -> EntityFactory.createUser(client, u))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

        var selfUser = usersMap.get(client.getSelfId());

        var chatsMap = chats.stream()
                .map(u -> EntityFactory.createChat(client, u, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

        Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(newMessages)
                .ofType(BaseMessageFields.class)
                .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                        .getStoreLayout().existMessage(message)))
                .flatMap(data -> {
                    Id peerId = Id.of(data.peerId());
                    var chat = getChatEntity(peerId, chatsMap, usersMap, selfUser);

                    var author = Optional.ofNullable(data.fromId())
                            .flatMap(p -> getPeerEntity(Id.of(p), chatsMap, usersMap))
                            .or(() -> Optional.ofNullable(chat)
                                    .filter(c -> c.getType() == Chat.Type.PRIVATE)
                                    .map(c -> ((PrivateChat) c).getUser()))
                            .orElse(null);

                    return Mono.justOrEmpty(chat)
                            .map(Chat::getId)
                            .switchIfEmpty(client.asInputPeer(peerId)
                                    .map(p -> Id.of(p, client.getSelfId())))
                            .switchIfEmpty(MappingUtil.unresolvedPeer(peerId))
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
                    var id = Optional.ofNullable(chatsMap.get(Id.ofChannel(u.channelId(), null)))
                            .map(c -> TlEntityUtil.toInputChannel(client.asResolvedInputPeer(c.getId()))) // must be present
                            .orElseThrow();

                    Integer upts = (upts = u.pts()) == null ? -1 : upts;
                    int dpts = Math.max(1, Math.max(upts, cpts));
                    int limit = getMaxChannelDifference();

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

    protected Flux<Event> handleChannelDifference(GetChannelDifference request, ChannelDifference diff) {
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
                .getStoreLayout().updateChannelPts(channelId.asLong(), newPts);

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
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                var selfUser = usersMap.get(client.getSelfId());

                var chatsMap = diff0.chats().stream()
                        .map(u -> EntityFactory.createChat(client, u, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                Flux<Event> messageCreateEvents = Flux.fromIterable(diff0.newMessages())
                        .ofType(BaseMessageFields.class)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message)))
                        .map(data -> {
                            var chat = getChatEntity(Id.of(data.peerId()), chatsMap, usersMap, selfUser);

                            var author = Optional.ofNullable(data.fromId())
                                    .flatMap(p -> getPeerEntity(Id.of(p), chatsMap, usersMap))
                                    .orElse(null);

                            var msg = EntityFactory.createMessage(client, data, channelId);

                            return new SendMessageEvent(client, msg, chat, author);
                        });

                Mono<Void> saveContacts = client.getMtProtoResources()
                        .getStoreLayout().onContacts(diff0.chats(), diff0.users());

                return updatePts.and(saveContacts)
                        .thenMany(Flux.fromIterable(diff0.otherUpdates()))
                        .flatMap(update -> UpdatesMapper.instance.handle(UpdateContext.create(
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
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                var selfUser = usersMap.get(client.getSelfId());

                var chatsMap = diff0.chats().stream()
                        .map(u -> EntityFactory.createChat(client, u, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                Flux<Event> messageCreateEvents = Flux.fromIterable(diff0.messages())
                        .ofType(BaseMessageFields.class)
                        .filterWhen(message -> BooleanUtils.not(client.getMtProtoResources()
                                .getStoreLayout().existMessage(message)))
                        .map(data -> {
                            var chat = getChatEntity(Id.of(data.peerId()), chatsMap, usersMap, selfUser);

                            var author = Optional.ofNullable(data.fromId())
                                    .flatMap(p -> getPeerEntity(Id.of(p), chatsMap, usersMap))
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

    protected Flux<Event> applyUpdate(UpdateContext<Update> ctx) {
        Flux<Event> mapUpdate = UpdatesMapper.instance.handle(ctx);

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

            if (channelId == -1) { // common pts
                Flux<Event> preApply = Flux.empty();
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

            long id = channelId;

            // If local channel pts is -1 just apply update
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
                        Mono<Void> updatePts = client.getMtProtoResources()
                                .getStoreLayout().updateChannelPts(id, ptsUpdate.pts());

                        if (justApplied.get()) {
                            log.debug("Updating channel state, pts: {}->{}", pts, ptsUpdate.pts());
                            return updatePts.thenMany(mapUpdate);
                        }

                        if (pts + ptsUpdate.ptsCount() < ptsUpdate.pts()) {
                            log.debug("Updates gap found for channel {}. Received pts: {}-{}, local pts: {}",
                                    id, ptsUpdate.pts() - ptsUpdate.ptsCount(), ptsUpdate.pts(), pts);

                            return client.getMtProtoResources().getStoreLayout().resolveChannel(id)
                                    .flatMapMany(c -> getChannelDifference(c, pts))
                                    .concatWith(mapUpdate);
                        } else if (pts + ptsUpdate.ptsCount() > ptsUpdate.pts()) {
                            return Flux.empty();
                        }

                        log.debug("Updating channel state, pts: {}->{}", pts, ptsUpdate.pts());
                        return updatePts.thenMany(mapUpdate);
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

    protected Flux<Event> getChannelDifference(InputChannel id, int pts) {
        int limit = getMaxChannelDifference();

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
                .flatMapMany(diff -> handleChannelDifference(request, diff))
                .onErrorResume(RpcException.isErrorMessage("CHANNEL_PRIVATE"), e -> Mono.empty());
    }

    @Nullable
    protected Chat getChatEntity(Id peer, Map<Id, Chat> chatsMap,
                               Map<Id, User> usersMap, @Nullable User selfUser) {
        switch (peer.getType()) {
            case CHAT:
            case CHANNEL:
                return chatsMap.get(peer);
            case USER:
                User data = usersMap.get(peer);
                return data != null ? new PrivateChat(client, data, selfUser) : null;
            default: throw new IllegalStateException();
        }
    }

    protected Optional<PeerEntity> getPeerEntity(Id peer, Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        switch (peer.getType()) {
            case USER: return Optional.ofNullable(usersMap.get(peer));
            case CHANNEL:
            case CHAT: return Optional.ofNullable(chatsMap.get(peer));
            default: throw new IllegalStateException();
        }
    }

    protected int getMaxChannelDifference() {
        return client.getAuthResources().isBot()
                ? options.maxBotChannelDifference
                : options.maxUserChannelDifference;
    }

    public static class Options {
        private static final int MAX_USER_CHANNEL_DIFFERENCE = 100;
        private static final int MAX_BOT_CHANNEL_DIFFERENCE  = 10000;
        private static final Duration DEFAULT_CHECKIN = Duration.ofMinutes(3);

        public final Duration checkin;
        public final int maxUserChannelDifference;
        public final int maxBotChannelDifference;

        public Options() {
            checkin = DEFAULT_CHECKIN;
            maxUserChannelDifference = MAX_USER_CHANNEL_DIFFERENCE;
            maxBotChannelDifference = MAX_BOT_CHANNEL_DIFFERENCE;
        }

        public Options(Duration checkin, int maxUserChannelDifference, int maxBotChannelDifference) {
            this.checkin = Objects.requireNonNull(checkin);
            this.maxUserChannelDifference = maxUserChannelDifference;
            this.maxBotChannelDifference = maxBotChannelDifference;
        }
    }
}
