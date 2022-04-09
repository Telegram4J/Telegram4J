package telegram4j.core.event.domain.inline;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

/** Event of ordinary inline button callback. */
public class CallbackQueryEvent extends CallbackEvent {
    private final int messageId;
    private final Chat chat;

    public CallbackQueryEvent(MTProtoTelegramClient client, long queryId, User user,
                              Chat chat, int msgId, long chatInstance,
                              @Nullable ByteBuf data, @Nullable String gameShortName) {
        super(client, queryId, user, chatInstance, data, gameShortName);
        this.messageId = msgId;
        this.chat = chat;
    }

    /**
     * Gets id of message where callback was triggered.
     *
     * @return The id of message where callback was triggered.
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Gets chat where this callback was triggered.
     *
     * @return The {@link Chat} where this callback was triggered.
     */
    public Chat getChat() {
        return chat;
    }

    @Override
    public String toString() {
        return "CallbackQueryEvent{" +
                "messageId=" + messageId +
                ", chat=" + chat +
                "} " + super.toString();
    }
}
