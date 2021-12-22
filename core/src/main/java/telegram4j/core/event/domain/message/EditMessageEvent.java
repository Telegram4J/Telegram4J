package telegram4j.core.event.domain.message;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.chat.Chat;

import java.util.Objects;
import java.util.Optional;

public class EditMessageEvent extends MessageEvent {

    private final Message newMessage;
    @Nullable
    private final Message oldMessage;
    @Nullable
    private final Chat chat;

    public EditMessageEvent(MTProtoTelegramClient client, Message newMessage,
                            @Nullable Message oldMessage, @Nullable Chat chat) {
        super(client);
        this.newMessage = Objects.requireNonNull(newMessage, "newMessage");
        this.oldMessage = oldMessage;
        this.chat = chat;
    }

    public Message getCurrentMessage() {
        return newMessage;
    }

    public Optional<Message> getOldMessage() {
        return Optional.ofNullable(oldMessage);
    }

    public Optional<Chat> getChat() {
        return Optional.ofNullable(chat);
    }

    @Override
    public String toString() {
        return "EditMessageEvent{" +
                "newMessage=" + newMessage +
                ", oldMessage=" + oldMessage +
                ", chat=" + chat +
                "} " + super.toString();
    }
}
