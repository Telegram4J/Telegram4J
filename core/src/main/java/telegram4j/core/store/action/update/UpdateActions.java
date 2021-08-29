package telegram4j.core.store.action.update;

import telegram4j.json.MessageData;

public final class UpdateActions {

    private UpdateActions() {}

    public static MessageCreateAction messageCreate(MessageData messageData) {
        return new MessageCreateAction(messageData);
    }

    public static MessageDeleteAction messageDelete(long chatId, long messageId) {
        return new MessageDeleteAction(chatId, messageId);
    }

    public static MessageUpdateAction messageUpdate(MessageData messageData) {
        return new MessageUpdateAction(messageData);
    }
}
