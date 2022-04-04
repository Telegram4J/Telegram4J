package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Information about forwarded message.
 */
public class MessageForwardHeader implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageFwdHeader data;

    public MessageForwardHeader(MTProtoTelegramClient client, telegram4j.tl.MessageFwdHeader data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets whether this message was imported from a foreign chat service.
     *
     * @see <a href="https://core.telegram.org/api/import">Message Importing</a>
     * @return {@code true} if message was imported from a foreign chat service.
     */
    public boolean isImported() {
        return data.imported();
    }

    /**
     * Gets id of the peer that originally sent the message, if present.
     *
     * @return The id of the peer that originally sent the message, if present.
     */
    public Optional<Id> getFromId() {
        return Optional.ofNullable(data.fromId()).map(Id::of);
    }

    /**
     * Gets name of the peer that originally sent the message, if present.
     *
     * @return The name of the peer that originally sent the message, if present.
     */
    public Optional<String> getFromName() {
        return Optional.ofNullable(data.fromName());
    }

    /**
     * Gets timestamp when originally was sent.
     *
     * @return The {@link Instant} when originally was sent.
     */
    public Instant getOriginalTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    /**
     * Gets id of channel message that was forwarded, if present.
     *
     * @return The id of channel message that was forwarded, if present.
     */
    public Optional<Integer> getChannelPostId() {
        return Optional.ofNullable(data.channelPost());
    }

    /**
     * Gets name of channel message's author, if present.
     *
     * @return The name of channel message's author, if present.
     */
    public Optional<String> getPostAuthor() {
        return Optional.ofNullable(data.postAuthor());
    }

    /**
     * Gets id of peer that was saved to self user, if present.
     *
     * @return The of peer that was saved to self user, if present.
     */
    public Optional<Id> getSavedFromPeerId() {
        return Optional.ofNullable(data.savedFromPeer()).map(Id::of);
    }

    /**
     * Gets id of message that was saved to self user, if present.
     *
     * @return The id of message that was saved to self user, if present.
     */
    public Optional<Integer> getSavedFromMessageId() {
        return Optional.ofNullable(data.savedFromMsgId());
    }

    // TODO: docs
    /**
     * Gets PSA type.
     *
     * @return The PSA type.
     */
    public Optional<String> getPsaType() {
        return Optional.ofNullable(data.psaType());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageForwardHeader that = (MessageForwardHeader) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageForwardHeader{" +
                "data=" + data +
                '}';
    }
}
