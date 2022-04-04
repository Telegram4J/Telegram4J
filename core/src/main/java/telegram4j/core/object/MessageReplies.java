package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Information about message replies.
 *
 * @see <a href="https://core.telegram.org/api/threads">Threads</a>
 */
public class MessageReplies implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageReplies data;

    public MessageReplies(MTProtoTelegramClient client, telegram4j.tl.MessageReplies data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets whether this information related to post comments.
     *
     * @return {@code true} if this information related to post comments.
     */
    public boolean isComments() {
        return data.comments();
    }

    /**
     * Gets number of replies in this thread.
     *
     * @return The number of replies in this thread
     */
    public int getReplies() {
        return data.replies();
    }

    /**
     * Gets channel pts of the message that started this thread.
     *
     * @return The channel pts of the message that started this thread.
     */
    public int getRepliesPts() {
        return data.repliesPts();
    }

    /**
     * Gets list of the last few comment posters ids, if present.
     *
     * @return The {@link List} of the last few comment posters ids, if present.
     */
    public Optional<List<Id>> getRecentRepliers() {
        return Optional.ofNullable(data.recentRepliers()).map(list -> list.stream()
                .map(Id::of)
                .collect(Collectors.toList()));
    }

    /**
     * Gets id of discussion supergroup, if present.
     *
     * @see <a href="https://core.telegram.org/api/discussion">Discussion Supergroups</a>
     * @return The id of discussion supergroup, if present.
     */
    public Optional<Id> getChannelId() {
        return Optional.ofNullable(data.channelId()).map(c -> Id.ofChannel(c, null));
    }

    /**
     * Gets id of the latest message in this thread, if present.
     *
     * @return The id of the latest message in this thread, if present.
     */
    public Optional<Integer> getMaxMessageId() {
        return Optional.ofNullable(data.maxId());
    }

    /**
     * Gets id of the latest read message in this thread, if present.
     *
     * @return The id of the latest read message in this thread, if present.
     */
    public Optional<Integer> getReadMaxMessageId() {
        return Optional.ofNullable(data.readMaxId());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageReplies that = (MessageReplies) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageReplies{" +
                "data=" + data +
                '}';
    }
}
