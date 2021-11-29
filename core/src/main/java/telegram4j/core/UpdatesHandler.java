package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.dispatcher.UpdateContext;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.request.updates.GetDifference;
import telegram4j.tl.updates.BaseDifference;
import telegram4j.tl.updates.DifferenceEmpty;
import telegram4j.tl.updates.DifferenceSlice;
import telegram4j.tl.updates.State;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.*;

public class UpdatesHandler {

    private static final Logger log = Loggers.getLogger(UpdatesHandler.class);

    private final MTProtoTelegramClient client;
    private final UpdatesHandlers updatesHandlers;

    private volatile int lastPts;
    private volatile int lastQts;
    private volatile int lastDate;
    private volatile int seq;

    public UpdatesHandler(MTProtoTelegramClient client, UpdatesHandlers updatesHandlers) {
        this.client = client;
        this.updatesHandlers = updatesHandlers;
    }

    private void applyState(State state) {
        lastPts = state.pts();
        lastQts = state.qts();
        lastDate = state.date();
        seq = state.seq();
    }

    private Flux<Event> getDifference() {
        return client.getSession()
                .sendEncrypted(GetDifference.builder()
                        .pts(lastPts)
                        .qts(lastQts)
                        .date(lastDate)
                        .build())
                .flatMapMany(difference -> {
                    switch (difference.identifier()) {
                        case DifferenceEmpty.ID:
                            DifferenceEmpty empty = (DifferenceEmpty) difference;
                            seq = empty.seq();
                            lastDate = empty.date();
                            return Mono.empty();
                        case BaseDifference.ID:
                            BaseDifference difference0 = (BaseDifference) difference;

                            applyState(difference0.state());

                            // currently, ignored
                            // difference0.newEncryptedMessages()

                            Map<Long, Chat> chatsMap = difference0.chats().stream()
                                    .collect(Collectors.toMap(Chat::id, Function.identity()));

                            Map<Long, User> usersMap = difference0.users().stream()
                                    .collect(Collectors.toMap(User::id, Function.identity()));

                            Flux<SendMessageEvent> messageCreateEvents = Flux.fromIterable(difference0.newMessages())
                                    .filter(message -> !(message instanceof MessageEmpty))
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

                            return Flux.fromIterable(difference0.otherUpdates())
                                    .<Event>flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                                            client, difference0.chats(),
                                            difference0.users(), update)))
                                    .concatWith(messageCreateEvents);
                        case DifferenceSlice.ID:
                            DifferenceSlice slice = (DifferenceSlice) difference;


                            return Mono.empty();
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown difference type: " + difference));
                    }
                });
    }

    public Flux<? extends Event> handle(Updates updates) {
        System.out.println("updates = " + updates);
        if (updates instanceof BaseUpdates) {
            BaseUpdates baseUpdates = (BaseUpdates) updates;

            return Flux.fromIterable(baseUpdates.updates())
                    .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                            client, baseUpdates.chats(), baseUpdates.users(), update)));
        }

        if (updates instanceof UpdatesTooLong) {
            return getDifference();
        }

        if (updates instanceof UpdatesCombined) {
            UpdatesCombined updatesCombined = (UpdatesCombined) updates;

            return Flux.fromIterable(updatesCombined.updates())
                    .flatMap(update -> updatesHandlers.handle(UpdateContext.create(
                            client, updatesCombined.chats(), updatesCombined.users(), update)));
        }

        if (updates instanceof UpdateShort) {
            UpdateShort updateShort = (UpdateShort) updates;

            return updatesHandlers.handle(UpdateContext.create(client, updateShort.update()));
        }

        return Flux.empty();
    }

}
