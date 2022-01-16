package telegram4j.core.event.domain.message;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.Message;

import java.util.List;

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

    public Id getChatId() {
        return chatId;
    }

    public boolean isScheduled() {
        return scheduled;
    }

    public List<Message> getDeletedMessages() {
        return deletedMessages;
    }

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
