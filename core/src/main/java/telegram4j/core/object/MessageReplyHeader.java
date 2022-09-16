package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;

import java.util.Objects;
import java.util.Optional;

/**
 * Reply information header.
 *
 * @see <a href="https://core.telegram.org/api/threads">Threads</a>
 */
public class MessageReplyHeader implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageReplyHeader data;

    public MessageReplyHeader(MTProtoTelegramClient client, telegram4j.tl.MessageReplyHeader data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets id of the message, to which is replying.
     *
     * @return The id of the message, to which is replying.
     */
    public int getReplyToMessageId() {
        return data.replyToMsgId();
    }

    /**
     * Gets id of the discussion group for replies of which the current user is not a member, if present.
     *
     * @see <a href="https://core.telegram.org/api/discussion">Discussion Groups</a>
     * @return The id of the discussion group.
     */
    public Optional<Id> getReplyToPeerId() {
        return Optional.ofNullable(data.replyToPeerId()).map(Id::of);
    }

    /**
     * Gets id of the first original reply message, if present.
     *
     * @return The id of the original reply message, if present.
     */
    public Optional<Integer> getReplyToTopId() {
        return Optional.ofNullable(data.replyToTopId());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageReplyHeader that = (MessageReplyHeader) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageReplyHeader{" +
                "data=" + data +
                '}';
    }
}
