package telegram4j.core.event.domain.inline;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.User;

/**
 * Subtype of bot interaction events.
 */
public abstract sealed class BotEvent extends Event
        permits CallbackEvent, InlineQueryEvent {

    protected BotEvent(MTProtoTelegramClient client) {
        super(client);
    }

    /**
     * Gets id of current query.
     *
     * @return The id of current query.
     */
    public abstract long getQueryId();

    /**
     * Gets {@link User} which starts this interaction.
     *
     * @return The {@link User} which starts this interaction.
     */
    public abstract User getUser();
}
