package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.ImmutableInputMessageID;

import java.util.List;
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
    private final Id chatId;

    public MessageReplyHeader(MTProtoTelegramClient client, telegram4j.tl.MessageReplyHeader data, Id chatId) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.chatId = Objects.requireNonNull(chatId);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public boolean isReplyToScheduled() {
        return data.replyToScheduled();
    }

    public boolean isForumTopic() {
        return data.forumTopic();
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
     * Requests to retrieve message to which message was replied.
     *
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getMessage() {
        return getMessage(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve message to which message was replied using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getMessage(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy).getMessages(chatId,
                List.of(ImmutableInputMessageID.of(data.replyToMsgId())));
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
