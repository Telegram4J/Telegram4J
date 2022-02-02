package telegram4j.core.event.domain.message;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.chat.Chat;

import java.util.Objects;
import java.util.Optional;

public class EditMessageEvent extends MessageEvent {

    private final Message newMessage;
    @Nullable
    private final Message oldMessage;
    @Nullable
    private final Chat chat;
    @Nullable
    private final PeerEntity author;

    public EditMessageEvent(MTProtoTelegramClient client, Message newMessage,
                            @Nullable Message oldMessage, @Nullable Chat chat,
                            @Nullable PeerEntity author) {
        super(client);
        this.newMessage = Objects.requireNonNull(newMessage, "newMessage");
        this.oldMessage = oldMessage;
        this.chat = chat;
        this.author = author;
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

    public Optional<PeerEntity> getAuthor() {
        return Optional.ofNullable(author);
    }

    @Override
    public String toString() {
        return "EditMessageEvent{" +
                "newMessage=" + newMessage +
                ", oldMessage=" + oldMessage +
                ", chat=" + chat +
                ", author=" + author +
                '}';
    }
}
