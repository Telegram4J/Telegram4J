package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.Id;

import java.util.Objects;

/**
 * Representation of certain peer reaction to the message.
 *
 * @see <a href="https://core.telegram.org/api/reactions">Message Reactions</a>
 */
public class MessagePeerReaction implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessagePeerReaction data;

    public MessagePeerReaction(MTProtoTelegramClient client, telegram4j.tl.MessagePeerReaction data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets whether the reaction should elicit a bigger and longer animation.
     *
     * @return {@code true} if the reaction should elicit a bigger and longer animation.
     */
    public boolean isBig() {
        return data.big();
    }

    /**
     * Gets whether the reaction wasn't yet marked as read by the <i>current</i> user.
     *
     * @return {@code true} if the reaction wasn't yet marked as read by the <i>current</i> user.
     */
    public boolean isUnread() {
        return data.unread();
    }

    /**
     * Gets id of peer that reacted to the message.
     *
     * @return The id of peer that reacted to the message.
     */
    public Id getPeerId() {
        return Id.of(data.peerId());
    }

    /**
     * Gets custom or default emoji reaction.
     *
     * @return The custom or default emoji reaction.
     */
    public Reaction getReaction() {
        return Objects.requireNonNull(EntityFactory.createReaction(data.reaction()));
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessagePeerReaction that = (MessagePeerReaction) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessagePeerReaction{" +
                "data=" + data +
                '}';
    }
}
