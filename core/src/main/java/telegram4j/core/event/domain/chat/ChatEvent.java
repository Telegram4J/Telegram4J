package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.chat.Chat;

/**
 * Subtype of chat/channel related events.
 *
 * <p>Chat Participant Events (Bot-Only)
 * <ul>
 *     <li>{@link ChatParticipantsUpdateEvent}: a batch event of participants updates.</li>
 * </ul>
 */
// TODO: docs for ChatParticipantAdminEvent
public abstract sealed class ChatEvent extends Event
        permits ChatParticipantUpdateEvent, ChatParticipantsUpdateEvent {

    protected ChatEvent(MTProtoTelegramClient client) {
        super(client);
    }

    public abstract Chat getChat();
}
