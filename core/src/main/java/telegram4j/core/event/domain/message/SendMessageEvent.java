package telegram4j.core.event.domain.message;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.chat.Chat;

import java.util.Objects;
import java.util.Optional;

public class SendMessageEvent extends MessageEvent {

    private final Message message;
    @Nullable
    private final Chat chat;
    @Nullable
    private final PeerEntity author;

    public SendMessageEvent(MTProtoTelegramClient client, Message message,
                            @Nullable Chat chat, @Nullable PeerEntity author) {
        super(client);
        this.message = Objects.requireNonNull(message, "message");
        this.chat = chat;
        this.author = author;
    }

    public Message getMessage() {
        return message;
    }

    public Optional<Chat> getChat() {
        return Optional.ofNullable(chat);
    }

    public Optional<PeerEntity> getAuthor() {
        return Optional.ofNullable(author);
    }

    @Override
    public String toString() {
        return "SendMessageEvent{" +
                "message=" + message +
                ", chat=" + chat +
                ", author=" + author +
                '}';
    }
}
