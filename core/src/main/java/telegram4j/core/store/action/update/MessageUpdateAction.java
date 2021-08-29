package telegram4j.core.store.action.update;

import telegram4j.core.store.StoreAction;
import telegram4j.json.MessageData;

public class MessageUpdateAction implements StoreAction<MessageData> {

    private final MessageData messageData;

    public MessageUpdateAction(MessageData messageData) {
        this.messageData = messageData;
    }

    public MessageData getMessageData() {
        return messageData;
    }
}
