package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.Message;

public class MessageCreateEvent extends Event {

    private final Message message;

    public MessageCreateEvent(TelegramClient client, Message message) {
        super(client);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "MessageCreateEvent{" +
                "message=" + message +
                '}';
    }
}
