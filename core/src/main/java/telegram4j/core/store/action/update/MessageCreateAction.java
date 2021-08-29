package telegram4j.core.store.action.update;

import telegram4j.core.store.StoreAction;
import telegram4j.json.MessageData;

public class MessageCreateAction implements StoreAction<Void> {

    private final MessageData messageData;

    public MessageCreateAction(MessageData messageData) {
        this.messageData = messageData;
    }

    public MessageData getMessageData() {
        return messageData;
    }
}
