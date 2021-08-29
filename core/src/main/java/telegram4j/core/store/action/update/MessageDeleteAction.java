package telegram4j.core.store.action.update;

import telegram4j.core.store.StoreAction;
import telegram4j.json.MessageData;

public class MessageDeleteAction implements StoreAction<MessageData> {

    private final long chatId;
    private final long messageId;

    public MessageDeleteAction(long chatId, long messageId) {
        this.chatId = chatId;
        this.messageId = messageId;
    }

    public long getChatId() {
        return chatId;
    }

    public long getMessageId() {
        return messageId;
    }
}
