package telegram4j.core.event.domain.message;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;

/**
 * Subtype of message related events.
 *
 * <ul>
 *     <li>{@link SendMessageEvent}: a new ordinary or scheduled message in the chat/channel/user was sent.</li>
 *     <li>{@link EditMessageEvent}: an message was updated, e.g., updated text, added reactions.</li>
 *     <li>{@link DeleteMessagesEvent}: a message or batch of messages was deleted.</li>
 * </ul>
 */
public abstract class MessageEvent extends Event {

    protected MessageEvent(MTProtoTelegramClient client) {
        super(client);
    }
}
