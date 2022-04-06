package telegram4j.core.event.domain.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;

/**
 * Subtype of chat/channel related events.
 *
 * <h3>Bot-Only Events</h3>
 * <ul>
 *     <li>{@link ChannelParticipantUpdateEvent}: a channel participant was updated, e.g., kicked/joined/made admin.</li>
 *     <li>{@link ChatParticipantAddEvent}: a new participant was joined to group chat.</li>
 *     <li>{@link ChatParticipantDeleteEvent}: a participant leaved group chat.</li>
 *     <li>{@link ChatParticipantUpdateEvent}: a status of participant was updated, e.g. participant made admin.</li>
 *     <li>{@link ChatParticipantsUpdateEvent}: a batch event of participants updates.</li>
 * </ul>
 */
public abstract class ChatEvent extends Event {

    protected ChatEvent(MTProtoTelegramClient client) {
        super(client);
    }
}
