package telegram4j.core.event.domain.message;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.Chat;
import telegram4j.tl.Message;
import telegram4j.tl.User;

import java.util.Optional;

public class SendMessageEvent extends MessageEvent {

    private final Message message;
    @Nullable
    private final Chat chat;
    @Nullable
    private final User user;

    public SendMessageEvent(MTProtoTelegramClient client,
                            Message message, @Nullable Chat chat,
                            @Nullable User user) {
        super(client);
        this.message = message;
        this.chat = chat;
        this.user = user;
    }

    public Message getMessage() {
        return message;
    }

    public Optional<Chat> getChat() {
        return Optional.ofNullable(chat);
    }

    public Optional<User> getUser() {
        return Optional.ofNullable(user);
    }

    @Override
    public String toString() {
        return "SendMessageEvent{" +
                "message=" + message +
                ", chat=" + chat +
                ", user=" + user +
                "} " + super.toString();
    }
}
