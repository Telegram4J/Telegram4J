package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.dispatcher.UpdateContext;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.Id;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.ImmutableGetDifference;
import telegram4j.tl.updates.*;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

public class UpdatesManager {

    private static final Logger log = Loggers.getLogger(UpdatesManager.class);

    private static final int MIN_CHANNEL_DIFFERENCE = 1;
    private static final int MAX_CHANNEL_DIFFERENCE = 100;
    private static final int MAX_BOT_CHANNEL_DIFFERENCE = 100000;

    private final MTProtoTelegramClient client;
    private final UpdatesHandlers updatesHandlers;

    private volatile int pts = -1;
    private volatile int qts = -1;
    private volatile int date = -1;
    private volatile int seq = -1;

    public UpdatesManager(MTProtoTelegramClient client, UpdatesHandlers updatesHandlers) {
        this.client = Objects.requireNonNull(client, "client");
        this.updatesHandlers = Objects.requireNonNull(updatesHandlers, "updatesHandlers");
    }

    public int getPts() {
        return pts;
    }

    public int getQts() {
        return qts;
    }

    public int getDate() {
        return date;
    }

    public int getSeq() {
        return seq;
    }

    public void setPts(int pts) {
        this.pts = pts;
    }

    public void setQts(int qts) {
        this.qts = qts;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public void setSeq(int seq) {
        this.seq = seq;
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
                        .doOnNext(this::applyStateLocal)
                        .thenReturn(state);
            }
            return Mono.just(state);
        })
        .flatMap(client.getMtProtoResources()
                .getStoreLayout()::updateState)
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

                return updatesHandlers.handle(UpdateContext.create(client, data.update()));
            }
            case BaseUpdates.ID: {
                BaseUpdates data = (BaseUpdates) updates;

                Flux<Event> preApply = Flux.empty();
                int updSeq = data.seq();
                if (updSeq != 0 && seq + 1 < updSeq) {
                    log.debug("Updates gap found. Received seq: {}-{}, local seq: {}", updSeq, updSeq + 1, seq);
                    preApply = getDifference();
                } else if (updSeq != 0 && seq + 1 > updSeq) {
                    return Flux.empty();
                }

                var usersMap = data.users().stream()
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));
                var chatsMap = data.chats().stream()
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                date = data.date();

                Flux<Event> events = Flux.fromIterable(data.updates())
                        .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)));

                return preApply.concatWith(events);
            }
            case UpdatesCombined.ID: {
                UpdatesCombined data = (UpdatesCombined) updates;

                Flux<Event> preApply = Flux.empty();
                int seqBegin = data.seqStart();
                if (seqBegin != 0 && seq + 1 < seqBegin) {
                    log.debug("Updates gap found. Received seq: {}-{}, local seq: {}", seqBegin, data.seq(), seq);
                    preApply = getDifference();
                } else if (seqBegin != 0 && seq + 1 > seqBegin) {
                    return Flux.empty();
                }

                var usersMap = data.users().stream()
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));
                var chatsMap = data.chats().stream()
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                seq = data.seq();
                date = data.date();

                Flux<Event> events0 = Flux.fromIterable(data.updates())
                        .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)));

                return preApply.concatWith(events0);
            }
            case UpdateShortChatMessage.ID: {
                UpdateShortChatMessage data = (UpdateShortChatMessage) updates;

                var message = BaseMessage.builder()
                        .out(data.out())
                        .mentioned(data.mentioned())
                        .mediaUnread(data.mediaUnread())
                        .silent(data.silent())
                        .id(data.id())
                        .date(data.date())
                        .fromId(ImmutablePeerUser.of(data.fromId()))
                        .peerId(ImmutablePeerUser.of(data.chatId()))
                        .fwdFrom(data.fwdFrom())
                        .viaBotId(data.viaBotId())
                        .replyTo(data.replyTo())
                        .entities(data.entities())
                        .ttlPeriod(data.ttlPeriod())
                        .message(data.message());

                return updatesHandlers.handle(UpdateContext.create(client, UpdateNewMessage.builder()
                        .message(message.build())
                        .pts(data.pts())
                        .ptsCount(data.ptsCount())
                        .build()));
            }
            case UpdateShortMessage.ID: {
                UpdateShortMessage data = (UpdateShortMessage) updates;

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

                return client.getMtProtoResources()
                        .getStoreLayout()
                        .getSelfId()
                        .map(selfId -> {
                            if (data.out()) {
                                message.peerId(ImmutablePeerUser.of(data.userId()));
                                message.fromId(ImmutablePeerUser.of(selfId));
                            } else {
                                message.peerId(ImmutablePeerUser.of(selfId));
                                message.fromId(ImmutablePeerUser.of(data.userId()));
                            }
                            return message.build();
                        })
                        .flatMapMany(msg -> updatesHandlers.handle(UpdateContext.create(client, UpdateNewMessage.builder()
                                .message(msg)
                                .pts(data.pts())
                                .ptsCount(data.ptsCount())
                                .build())));
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
            if (pts == state.pts() && qts == state.qts() && state.seq() == seq) {
                return Mono.empty();
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
                                    .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));
                            var usersMap = difference0.users().stream()
                                    .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));

                            Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(difference0.newMessages())
                                    .ofType(BaseMessageFields.class)
                                    .flatMap(data -> {
                                        long id = getRawPeerId(data.peerId());
                                        var selfUser = usersMap.values().stream()
                                                .filter(u -> u.identifier() == BaseUser.ID && ((BaseUser) u).self())
                                                .map(b -> (BaseUser) b)
                                                .findFirst()
                                                .orElse(null);
                                        var chat = Optional.<TlObject>ofNullable(chatsMap.get(id))
                                                .or(() -> Optional.ofNullable(usersMap.get(id)))
                                                .map(c -> EntityFactory.createChat(client, c, selfUser))
                                                .orElse(null);

                                        var user = Optional.ofNullable(data.fromId())
                                                .filter(u -> u.identifier() == InputPeerUser.ID) // TODO: check other variants
                                                .map(p -> usersMap.get(getRawPeerId(p)))
                                                .filter(u -> u.identifier() == BaseUser.ID)
                                                .map(d -> new User(client, (BaseUser) d))
                                                .orElse(null);

                                        return Mono.justOrEmpty(Optional.ofNullable(chat).map(Chat::getId))
                                                .switchIfEmpty(toId(data.peerId()))
                                                .map(i -> EntityFactory.createMessage(client, data, i))
                                                .map(m -> new SendMessageEvent(client, m, chat, user));
                                    });

                            Flux<Event> applyChannelDifference = Mono.justOrEmpty(difference0.otherUpdates().stream()
                                            .filter(u -> u.identifier() == UpdateChannelTooLong.ID)
                                            .map(u -> (UpdateChannelTooLong) u)
                                            .findFirst())
                                    .zipWhen(u -> client.getMtProtoResources()
                                            .getStoreLayout()
                                            .getChatFullById(u.channelId())
                                            .map(ChatFull::fullChat)
                                            .ofType(ChannelFull.class)
                                            .map(ChannelFull::pts)
                                            .defaultIfEmpty(-1))
                                    .filter(TupleUtils.predicate((u, c) -> c == -1 || u.pts() == null ||
                                            Objects.requireNonNull(u.pts()) > c))
                                    .flatMapMany(TupleUtils.function((u, cpts) -> {
                                        InputChannel id = Optional.ofNullable(chatsMap.get(u.channelId()))
                                                .filter(ch -> ch.identifier() == Channel.ID)
                                                .map(ch -> (Channel) ch)
                                                .map(ch -> ImmutableBaseInputChannel.of(ch.id(),
                                                        Objects.requireNonNull(ch.accessHash())))
                                                .orElseThrow();

                                        Integer upts = u.pts();
                                        int dpts;
                                        if (cpts != -1) {
                                            dpts = cpts;
                                        } else {
                                            dpts = upts != null ? upts : -1;
                                        }

                                        int limit = client.isBot()
                                                ? MAX_BOT_CHANNEL_DIFFERENCE
                                                : MAX_CHANNEL_DIFFERENCE;
                                        if (dpts == -1) {
                                            limit = MIN_CHANNEL_DIFFERENCE;
                                            dpts = 1;
                                        }

                                        return client.getServiceHolder()
                                                .getUpdatesService()
                                                .getChannelDifference(GetChannelDifference.builder()
                                                        .force(true)
                                                        .channel(id)
                                                        .pts(dpts)
                                                        .filter(ChannelMessagesFilterEmpty.instance())
                                                        .limit(limit)
                                                        .build())
                                                .flatMapMany(this::handleChannelDifference);
                                    }));

                            Flux<Event> concatedUpdates = Flux.fromIterable(difference0.otherUpdates())
                                    .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                            client, chatsMap, usersMap, update)))
                                    .concatWith(messageCreateEvents)
                                    .concatWith(applyChannelDifference);

                            return applyState(difference0.state())
                                    .thenMany(concatedUpdates);
                        }
                        case DifferenceSlice.ID: {
                            DifferenceSlice difference0 = (DifferenceSlice) difference;

                            var chatsMap = difference0.chats().stream()
                                    .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                            var usersMap = difference0.users().stream()
                                    .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));

                            Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(difference0.newMessages())
                                    .ofType(BaseMessageFields.class)
                                    .flatMap(data -> {
                                        long id = getRawPeerId(data.peerId());
                                        var selfUser = usersMap.values().stream()
                                                .filter(u -> u.identifier() == BaseUser.ID && ((BaseUser) u).self())
                                                .map(b -> (BaseUser) b)
                                                .findFirst()
                                                .orElse(null);
                                        var chat = Optional.<TlObject>ofNullable(chatsMap.get(id))
                                                .or(() -> Optional.ofNullable(usersMap.get(id)))
                                                .map(c -> EntityFactory.createChat(client, c, selfUser))
                                                .orElse(null);

                                        var user = Optional.ofNullable(data.fromId())
                                                .filter(u -> u.identifier() == InputPeerUser.ID) // TODO: check other variants
                                                .map(p -> usersMap.get(getRawPeerId(p)))
                                                .filter(u -> u.identifier() == BaseUser.ID)
                                                .map(d -> new User(client, (BaseUser) d))
                                                .orElse(null);

                                        return Mono.justOrEmpty(Optional.ofNullable(chat).map(Chat::getId))
                                                .switchIfEmpty(toId(data.peerId()))
                                                .map(i -> EntityFactory.createMessage(client, data, i))
                                                .map(m -> new SendMessageEvent(client, m, chat, user));
                                    });

                            State intermediateState = difference0.intermediateState();

                            return Flux.fromIterable(difference0.otherUpdates())
                                    .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                            client, chatsMap, usersMap, update)))
                                    .concatWith(messageCreateEvents)
                                    .transform(f -> getDifference(intermediateState.pts(),
                                            intermediateState.qts(), intermediateState.date())
                                            .concatWith(f));
                        }
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown difference type: " + difference));
                    }
                });
    }

    private Flux<Event> handleChannelDifference(ChannelDifference diff) {
        if (log.isTraceEnabled()) {
            log.trace("channel difference: {}", diff);
        }
        switch (diff.identifier()) {
            // TODO: deal with the processing and checking of pts updates
            case BaseChannelDifference.ID: {
                BaseChannelDifference diff0 = (BaseChannelDifference) diff;
                return Flux.empty();
            }
            case ChannelDifferenceEmpty.ID: {
                ChannelDifferenceEmpty diff0 = (ChannelDifferenceEmpty) diff;
                return Flux.empty();
            }
            case ChannelDifferenceTooLong.ID: {
                ChannelDifferenceTooLong diff0 = (ChannelDifferenceTooLong) diff;
                return Flux.empty();
            }
            default:
                return Flux.error(new IllegalArgumentException("Unknown channel difference type: " + diff));
        }
    }

    private Mono<Id> toId(Peer peer) {
        long id = TlEntityUtil.getRawPeerId(peer);
        return Mono.defer(() -> {
            switch (peer.identifier()) {
                case PeerChannel.ID: return client.getMtProtoResources().getStoreLayout()
                        .resolveChannel(id).map(TlEntityUtil::toInputPeer);
                case PeerChat.ID:
                    return Mono.just(ImmutableInputPeerChat.of(id));
                case PeerUser.ID: return client.getMtProtoResources()
                        .getStoreLayout().resolveUser(id);
                default: return Mono.error(new IllegalArgumentException("Unknown peer type: " + peer));
            }
        })
        .map(inputPeer -> {
            switch (peer.identifier()) {
                // InputPeerUserFromMessage or InputPeerChannelFromMessage currently cannot be mapped
                case InputPeerChannel.ID:
                    InputPeerChannel inputPeerChannel = (InputPeerChannel) peer;
                    return Id.ofChannel(inputPeerChannel.channelId(), inputPeerChannel.accessHash());
                case InputPeerChat.ID:
                    InputPeerChat inputPeerChat = (InputPeerChat) peer;
                    return Id.ofChat(inputPeerChat.chatId());
                case InputPeerSelf.ID: return Id.ofUser(id, null);
                case InputPeerUser.ID:
                    InputPeerUser inputPeerUser = (InputPeerUser) peer;
                    return Id.ofUser(inputPeerUser.userId(), inputPeerUser.accessHash());
                default: throw new IllegalArgumentException("Unknown input peer type: " + peer);
            }
        });
    }
}
