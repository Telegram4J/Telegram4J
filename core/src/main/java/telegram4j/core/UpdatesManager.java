package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.request.updates.ImmutableGetDifference;
import telegram4j.tl.updates.BaseDifference;
import telegram4j.tl.updates.DifferenceEmpty;
import telegram4j.tl.updates.DifferenceSlice;
import telegram4j.tl.updates.State;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

public class UpdatesManager {

    private static final Logger log = Loggers.getLogger(UpdatesManager.class);

    private final MTProtoTelegramClient client;
    private final UpdatesHandlers updatesHandlers;

    private volatile int pts;
    private volatile int qts;
    private volatile int date;
    private volatile int seq;

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
                .flatMap(state -> client.getMtProtoResources()
                .getStoreLayout().getCurrentState()
                .switchIfEmpty(client.getMtProtoResources()
                        .getStoreLayout().updateState(state)
                        .thenReturn(state))
                .doOnNext(this::applyStateLocal)
                .thenMany(getDifference())
                .doOnNext(client.getMtProtoResources()
                        .getEventDispatcher()::publish)
                .then());
    }

    public Flux<Event> handle(Updates updates) {
        switch (updates.identifier()) {
            case UpdatesTooLong.ID:
                return getDifference();
            case UpdateShort.ID: {
                UpdateShort updateShort = (UpdateShort) updates;

                Flux<?> preApply = Flux.empty();
                date = updateShort.date();
                if (updateShort.update() instanceof PtsUpdate) {
                    PtsUpdate ptsUpdate = (PtsUpdate) updateShort.update();

                    if (pts + ptsUpdate.ptsCount() > ptsUpdate.pts()) { // ignore
                        return Flux.empty();
                    } else if (pts + ptsUpdate.ptsCount() < ptsUpdate.pts()) { // fill gap
                        preApply = getDifference();
                    } else { // apply
                        pts = ptsUpdate.pts();
                    }
                }

                return preApply.thenMany(updatesHandlers.handle(UpdateContext.create(client, updateShort.update())));
            }
            case BaseUpdates.ID: {
                BaseUpdates baseUpdates = (BaseUpdates) updates;

                Flux<?> preApply = Flux.empty();
                int updSeq = baseUpdates.seq();
                if (updSeq != 0 && seq + 1 < updSeq) {
                    preApply = getDifference();
                }

                var usersMap = baseUpdates.users().stream()
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));
                var chatsMap = baseUpdates.chats().stream()
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                seq = updSeq;
                date = baseUpdates.date();

                Flux<Event> events = Flux.fromIterable(baseUpdates.updates())
                        .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)));

                return preApply.thenMany(events);
            }
            case UpdatesCombined.ID: {
                UpdatesCombined updatesCombined = (UpdatesCombined) updates;

                Flux<?> preApply0 = Flux.empty();
                int seqBegin = updatesCombined.seqStart();
                if (seqBegin != 0 && seq + 1 < seqBegin) {
                    preApply0 = getDifference();
                }

                var usersMap = updatesCombined.users().stream()
                        .collect(Collectors.toMap(telegram4j.tl.User::id, Function.identity()));
                var chatsMap = updatesCombined.chats().stream()
                        .collect(Collectors.toMap(telegram4j.tl.Chat::id, Function.identity()));

                seq = updatesCombined.seq();
                date = updatesCombined.date();

                Flux<Event> events0 = Flux.fromIterable(updatesCombined.updates())
                        .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                client, chatsMap, usersMap, update)));

                return preApply0.thenMany(events0);
            }
            default:
                return Flux.empty();
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
                        case DifferenceEmpty.ID:
                            DifferenceEmpty empty = (DifferenceEmpty) difference;

                            this.seq = empty.seq();
                            this.date = empty.date();
                            return Mono.empty();
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
                                        var chat = Optional.<TlObject>ofNullable(chatsMap.get(id))
                                                .or(() -> Optional.ofNullable(usersMap.get(id)))
                                                .map(c -> EntityFactory.createChat(client, c))
                                                .orElse(null);

                                        var user = Optional.ofNullable(data.fromId())
                                                .filter(u -> u.identifier() == InputPeerUser.ID) // TODO: check other variants
                                                .map(p -> usersMap.get(getRawPeerId(p)))
                                                .filter(u -> u.identifier() == BaseUser.ID)
                                                .map(d -> new User(client, (BaseUser) d))
                                                .orElse(null);

                                        return Mono.justOrEmpty(Optional.ofNullable(chat).map(Chat::getId))
                                                .switchIfEmpty(client.getServiceHolder()
                                                        .getMessageService().getInputPeer(data.peerId())
                                                        .map(p -> peerToId(p, id)))
                                                .map(i -> EntityFactory.createMessage(client, data, i))
                                                .map(m -> new SendMessageEvent(client, m, chat, user));
                                    });

                            Flux<Event> concatedUpdates = Flux.fromIterable(difference0.otherUpdates())
                                    .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                            client, chatsMap, usersMap, update)))
                                    .concatWith(messageCreateEvents);

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
                                        Peer peer = data.peerId();
                                        long id = getRawPeerId(peer);
                                        var chat = Optional.<TlObject>ofNullable(chatsMap.get(id))
                                                .or(() -> Optional.ofNullable(usersMap.get(id)))
                                                .map(c -> EntityFactory.createChat(client, c))
                                                .orElse(null);

                                        var user = Optional.ofNullable(data.fromId())
                                                .filter(u -> u.identifier() == InputPeerUser.ID) // TODO: check other variants
                                                .map(p -> usersMap.get(getRawPeerId(p)))
                                                .filter(u -> u.identifier() == BaseUser.ID)
                                                .map(d -> new User(client, (BaseUser) d))
                                                .orElse(null);

                                        return Mono.justOrEmpty(Optional.ofNullable(chat).map(Chat::getId))
                                                .switchIfEmpty(client.getServiceHolder()
                                                        .getMessageService().getInputPeer(peer)
                                                        .map(p -> peerToId(p, id)))
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

    static Id peerToId(InputPeer peer, long resolvedId) {
        switch (peer.identifier()) {
            case InputPeerChannel.ID:
                InputPeerChannel inputPeerChannel = (InputPeerChannel) peer;
                return Id.ofChannel(inputPeerChannel.channelId(), inputPeerChannel.accessHash());
            case InputPeerChannelFromMessage.ID:
                var minInputPeerChannel = (InputPeerChannelFromMessage) peer;
                Long channelAccessHash0 = minInputPeerChannel.peer() instanceof InputPeerChannel
                        ? ((InputPeerChannel) minInputPeerChannel.peer()).accessHash()
                        : null;

                return Id.ofChannel(minInputPeerChannel.channelId(), channelAccessHash0);
            case InputPeerChat.ID:
                InputPeerChat inputPeerChat = (InputPeerChat) peer;
                return Id.ofChat(inputPeerChat.chatId());
            case InputPeerSelf.ID: return Id.ofUser(resolvedId, null);
            case InputPeerUser.ID:
                InputPeerUser inputPeerUser = (InputPeerUser) peer;
                return Id.ofUser(inputPeerUser.userId(), inputPeerUser.accessHash());
            case InputPeerUserFromMessage.ID:
                var minInputPeerUser = (InputPeerUserFromMessage) peer;
                Long channelAccessHash = minInputPeerUser.peer() instanceof InputPeerChannel
                        ? ((InputPeerChannel) minInputPeerUser.peer()).accessHash()
                        : null;

                return Id.ofUser(minInputPeerUser.userId(), channelAccessHash);
            default: throw new IllegalArgumentException("Unknown input peer type: " + peer);
        }
    }
}
