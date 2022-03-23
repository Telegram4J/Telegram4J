package telegram4j.core.event.domain.message;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.Message;

import java.util.List;

/** Event of single or batch delete of ordinal or scheduled messages. */
public class DeleteMessagesEvent extends MessageEvent {

    private final Id chatId;
    private final boolean scheduled;
    private final List<Message> deletedMessages;
    private final List<Integer> deleteMessagesIds;

    public DeleteMessagesEvent(MTProtoTelegramClient client, Id chatId, boolean scheduled,
                               List<Message> deletedMessages, List<Integer> deleteMessagesIds) {
        super(client);
        this.chatId = chatId;
        this.scheduled = scheduled;
        this.deletedMessages = deletedMessages;
        this.deleteMessagesIds = deleteMessagesIds;
    }

    /**
     * Gets id of the chat/channel where messages was deleted.
     *
     * @return The id of the chat/channel where messages was deleted.
     */
    public Id getChatId() {
        return chatId;
    }

    /**
     * Gets whether deleted messages were scheduled.
     *
     * @return {@code true} if deleted messages were scheduled.
     */
    public boolean isScheduled() {
        return scheduled;
    }

    /**
     * Gets {@link List} with found deleted {@link Message messages}.
     *
     * @return The list with found deleted messages.
     */
    public List<Message> getDeletedMessages() {
        return deletedMessages;
    }

    /**
     * Gets {@link List} of deleted message ids.
     *
     * @return The list of deleted message ids.
     */
    public List<Integer> getDeleteMessagesIds() {
        return deleteMessagesIds;
    }

    @Override
    public String toString() {
        return "DeleteMessagesEvent{" +
                "chatId=" + chatId +
                ", scheduled=" + scheduled +
                ", deletedMessages=" + deletedMessages +
                ", deleteMessagesIds=" + deleteMessagesIds +
                '}';
    }
}
