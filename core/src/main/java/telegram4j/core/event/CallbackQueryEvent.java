package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.CallbackQuery;

public class CallbackQueryEvent extends Event {

    private final CallbackQuery callbackQuery;

    public CallbackQueryEvent(TelegramClient client, CallbackQuery callbackQuery) {
        super(client);
        this.callbackQuery = callbackQuery;
    }

    public CallbackQuery getCallbackQuery() {
        return callbackQuery;
    }

    @Override
    public String toString() {
        return "CallbackQueryEvent{" +
                "callbackQuery=" + callbackQuery +
                "} " + super.toString();
    }
}
