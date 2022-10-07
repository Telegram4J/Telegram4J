package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.RetrievalUtil;
import telegram4j.core.object.chat.SupergroupChat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
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
     * @return The mutable {@link Set} of the last few comment posters ids, if present.
     */
    public Set<Id> getRecentRepliers() {
        return Optional.ofNullable(data.recentRepliers())
                .map(list -> list.stream()
                        .map(Id::of)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /**
     * Gets id of discussion supergroup, if present.
     *
     * @see <a href="https://core.telegram.org/api/discussion">Discussion Supergroups</a>
     * @return The id of discussion supergroup, if present.
     */
    public Optional<Id> getDiscussionChannelId() {
        return Optional.ofNullable(data.channelId()).map(c -> Id.ofChannel(c, null));
    }

    /**
     * Requests to retrieve discussion channel.
     *
     * @return An {@link Mono} emitting on successful completion the {@link SupergroupChat discussion channel}.
     */
    public Mono<SupergroupChat> getDiscussionChannel() {
        return getDiscussionChannel(RetrievalUtil.IDENTITY);
    }

    /**
     * Requests to retrieve discussion channel using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link SupergroupChat discussion channel}.
     */
    public Mono<SupergroupChat> getDiscussionChannel(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getDiscussionChannelId())
                .flatMap(id -> client.withRetrievalStrategy(strategy).getChatById(id))
                .cast(SupergroupChat.class);
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
