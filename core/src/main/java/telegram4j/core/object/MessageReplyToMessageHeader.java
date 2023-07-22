package telegram4j.core.object;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.BaseMessageReplyHeader;
import telegram4j.tl.ImmutableInputMessageID;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MessageReplyToMessageHeader extends MessageReplyHeader {
    private final telegram4j.tl.BaseMessageReplyHeader data;
    private final Id chatId;

    public MessageReplyToMessageHeader(MTProtoTelegramClient client, telegram4j.tl.BaseMessageReplyHeader data, Id chatId) {
        super(client);
        this.data = Objects.requireNonNull(data);
        this.chatId = Objects.requireNonNull(chatId);
    }

    // TODO: ???
    public boolean isReplyToScheduled() {
        return data.replyToScheduled();
    }

    /**
     * Gets whether message was sent in channel forum topic.
     *
     * @return {@code true} if message was sent in channel forum topic.
     */
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
    public String toString() {
        return "MessageReplyToMessageHeader{" +
                "data=" + data +
                '}';
    }
}
