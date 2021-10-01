package telegram4j.core.event;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.core.object.Message;

import java.util.Optional;

public class ChannelPostUpdateEvent extends Event {

    private final Message currentMessage;

    @Nullable
    private final Message oldMessage;

    public ChannelPostUpdateEvent(TelegramClient client, Message currentMessage, @Nullable Message oldMessage) {
        super(client);
        this.currentMessage = currentMessage;
        this.oldMessage = oldMessage;
    }

    public Message getCurrentMessage() {
        return currentMessage;
    }

    public Optional<Message> getOldMessage() {
        return Optional.ofNullable(oldMessage);
    }

    @Override
    public String toString() {
        return "ChannelPostUpdateEvent{" +
                "currentMessage=" + currentMessage +
                ", oldMessage=" + oldMessage +
                "} " + super.toString();
    }
}
