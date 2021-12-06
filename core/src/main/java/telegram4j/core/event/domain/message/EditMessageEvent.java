package telegram4j.core.event.domain.message;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.Chat;
import telegram4j.tl.Message;
import telegram4j.tl.User;

import java.util.Optional;

public class EditMessageEvent extends MessageEvent {

    private final Message newMessage;
    @Nullable
    private final Message oldMessage;
    @Nullable
    private final Chat chat;
    @Nullable
    private final User user;

    public EditMessageEvent(MTProtoTelegramClient client, Message newMessage,
                            @Nullable Message oldMessage,
                            @Nullable Chat chat, @Nullable User user) {
        super(client);
        this.newMessage = newMessage;
        this.oldMessage = oldMessage;
        this.chat = chat;
        this.user = user;
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

    public Optional<User> getUser() {
        return Optional.ofNullable(user);
    }

    @Override
    public String toString() {
        return "EditMessageEvent{" +
                "newMessage=" + newMessage +
                ", oldMessage=" + oldMessage +
                ", chat=" + chat +
                ", user=" + user +
                "} " + super.toString();
    }
}
