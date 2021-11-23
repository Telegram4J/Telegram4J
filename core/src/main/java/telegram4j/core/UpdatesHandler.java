package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.Event;
import telegram4j.core.event.dispatcher.UpdateContext;
import telegram4j.core.event.dispatcher.UpdatesHandlers;
import telegram4j.tl.*;

public class UpdatesHandler {

    private static final Logger log = Loggers.getLogger(UpdatesHandler.class);

    private final MTProtoTelegramClient client;
    private final UpdatesHandlers updatesHandlers;

    public UpdatesHandler(MTProtoTelegramClient client, UpdatesHandlers updatesHandlers) {
        this.client = client;
        this.updatesHandlers = updatesHandlers;
    }

    public Flux<Event> handle(Updates updates) {
        log.debug(updates.toString());

        if (updates instanceof BaseUpdates) {
            BaseUpdates baseUpdates = (BaseUpdates) updates;

            return Flux.fromIterable(baseUpdates.updates())
                    .flatMap(update -> updatesHandlers.handle(new UpdateContext<>(
                            client, baseUpdates.chats(), baseUpdates.users(), update)));
        }

        if (updates instanceof UpdateShortMessage) {
            UpdateShortMessage updateShortMessage = (UpdateShortMessage) updates;

            // TODO
        }

        if (updates instanceof UpdatesTooLong) {
            UpdatesTooLong updatesTooLong = (UpdatesTooLong) updates;

            if (log.isDebugEnabled()) {
                log.debug("Handling updates too long: {}", updatesTooLong);
            }
        }

        if (updates instanceof UpdatesCombined) {
            UpdatesCombined updatesCombined = (UpdatesCombined) updates;

            return Flux.fromIterable(updatesCombined.updates())
                    .flatMap(update -> updatesHandlers.handle(new UpdateContext<>(
                            client, updatesCombined.chats(), updatesCombined.users(), update)));
        }

        if (updates instanceof UpdateShort) {
            UpdateShort updateShort = (UpdateShort) updates;

            return updatesHandlers.handle(new UpdateContext<>(client, updateShort.update()));
        }

        return Flux.empty();
    }

}
