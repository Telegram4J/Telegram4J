package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.dispatcher.UpdateContext;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.tl.*;
import telegram4j.tl.request.updates.GetDifference;
import telegram4j.tl.request.updates.GetState;
import telegram4j.tl.updates.*;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.peerId;

public class UpdatesManager {

    private static final Logger log = Loggers.getLogger(UpdatesManager.class);

    private final MTProtoTelegramClient client;
    private final UpdatesHandlers updatesHandlers;

    private volatile int pts;
    private volatile int qts;
    private volatile int date;
    private volatile int seq;

    public UpdatesManager(MTProtoTelegramClient client, UpdatesHandlers updatesHandlers) {
        this.client = client;
        this.updatesHandlers = updatesHandlers;
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
        return client.getMtProtoClient().sendAwait(GetState.instance())
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
        // *embedded* message updates must be handled not here.
        switch (updates.identifier()) {
            case UpdatesTooLong.ID:
                return getDifference();
            case UpdateShort.ID:
                UpdateShort updateShort = (UpdateShort) updates;

                date = updateShort.date();

                return updatesHandlers.handle(UpdateContext.create(client, updateShort.update()));
            case BaseUpdates.ID:
                BaseUpdates baseUpdates = (BaseUpdates) updates;

                Flux<?> preApply = Flux.empty();
                int updSeq = baseUpdates.seq();
                if (updSeq != 0 && seq + 1 < updSeq) {
                    preApply = getDifference();
                }

                seq = updSeq;
                date = baseUpdates.date();

                Flux<Event> events = Flux.fromIterable(baseUpdates.updates())
                        .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                client, baseUpdates.chats(), baseUpdates.users(), update)));

                return preApply.thenMany(events);
            case UpdatesCombined.ID:
                UpdatesCombined updatesCombined = (UpdatesCombined) updates;

                Flux<?> preApply0 = Flux.empty();
                int seqBegin = updatesCombined.seqStart();
                if (seqBegin != 0 && seq + 1 < seqBegin) {
                    preApply0 = getDifference();
                }

                seq = updatesCombined.seq();
                date = updatesCombined.date();

                Flux<Event> events0 = Flux.fromIterable(updatesCombined.updates())
                        .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                client, updatesCombined.chats(), updatesCombined.users(), update)));

                return preApply0.thenMany(events0);
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
        return client.getMtProtoClient()
                .sendAwait(GetDifference.builder()
                        .pts(pts)
                        .qts(qts)
                        .date(date)
                        .build())
                .flatMapMany(difference -> {
                    log.info("difference: {}", difference);
                    switch (difference.identifier()) {
                        case DifferenceEmpty.ID:
                            DifferenceEmpty empty = (DifferenceEmpty) difference;

                            this.seq = empty.seq();
                            this.date = empty.date();
                            return Mono.empty();
                        case BaseDifference.ID:
                            BaseDifference difference0 = (BaseDifference) difference;

                            // currently, ignored
                            // difference0.newEncryptedMessages()

                            Map<Long, Chat> chatsMap = difference0.chats().stream()
                                    .collect(Collectors.toMap(Chat::id, Function.identity()));

                            Map<Long, User> usersMap = difference0.users().stream()
                                    .collect(Collectors.toMap(User::id, Function.identity()));

                            Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(difference0.newMessages())
                                    .map(message -> {
                                        Peer peerId = message instanceof MessageService
                                                ? ((MessageService) message).peerId()
                                                : ((BaseMessage) message).peerId();

                                        User user = null;
                                        Chat chat = null;
                                        long id = peerId(peerId);
                                        if (peerId instanceof PeerChat || peerId instanceof PeerChannel) {
                                            chat = chatsMap.get(id);
                                        } else { // PeerUser
                                            user = usersMap.get(id);
                                        }

                                        return new SendMessageEvent(client, message, chat, user);
                                    });

                            Flux<Event> concatedUpdates = Flux.fromIterable(difference0.otherUpdates())
                                    .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                            client, difference0.chats(),
                                            difference0.users(), update)))
                                    .concatWith(messageCreateEvents);

                            return applyState(difference0.state())
                                    .thenMany(concatedUpdates);
                        case DifferenceSlice.ID:
                            DifferenceSlice slice = (DifferenceSlice) difference;

                            Map<Long, Chat> chatsMap0 = slice.chats().stream()
                                    .collect(Collectors.toMap(Chat::id, Function.identity()));

                            Map<Long, User> usersMap0 = slice.users().stream()
                                    .collect(Collectors.toMap(User::id, Function.identity()));

                            Flux<SendMessageEvent> messageCreateEvents0 = Flux.fromIterable(slice.newMessages())
                                    .map(message -> {
                                        Peer peerId = message instanceof MessageService
                                                ? ((MessageService) message).peerId()
                                                : ((BaseMessage) message).peerId();

                                        User user = null;
                                        Chat chat = null;
                                        long id = peerId(peerId);
                                        if (peerId instanceof PeerChat || peerId instanceof PeerChannel) {
                                            chat = chatsMap0.get(id);
                                        } else { // PeerUser
                                            user = usersMap0.get(id);
                                        }

                                        return new SendMessageEvent(client, message, chat, user);
                                    });

                            State intermediateState = slice.intermediateState();

                            return Flux.fromIterable(slice.otherUpdates())
                                    .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                            client, slice.chats(), slice.users(), update)))
                                    .concatWith(messageCreateEvents0)
                                    .transform(f -> getDifference(intermediateState.pts(),
                                            intermediateState.qts(), intermediateState.date())
                                            .concatWith(f));
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown difference type: " + difference));
                    }
                });
    }
}
