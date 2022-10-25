package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.chat.Chat;

/**
 * Subtype of chat/channel related events.
 *
 * <p>Chat Participant Events (Bot-Only)
 * <ul>
 *     <li>{@link ChatParticipantAddEvent}: a new participant was joined to group chat.</li>
 *     <li>{@link ChatParticipantDeleteEvent}: a participant leaved group chat.</li>
 *     <li>{@link ChatParticipantUpdateEvent}: a status of chat/channel participant was updated, e.g. participant made admin.</li>
 *     <li>{@link ChatParticipantsUpdateEvent}: a batch event of participants updates.</li>
 * </ul>
 */
// TODO: docs for ChatParticipantAdminEvent
public abstract class ChatEvent extends Event {

    protected ChatEvent(MTProtoTelegramClient client) {
        super(client);
    }

    public abstract Chat getChat();
}
