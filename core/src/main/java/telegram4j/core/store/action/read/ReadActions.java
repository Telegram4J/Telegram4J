package telegram4j.core.store.action.read;

public final class ReadActions {

    private ReadActions() {}

    public static GetMessage getMessageById(long chatId, long messageId) {
        return new GetMessage(chatId, messageId);
    }
}
