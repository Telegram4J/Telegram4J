package telegram4j.core.event.domain.message;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.Message;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Optional;

/** Event of single or batch delete of ordinal or scheduled messages. */
public class DeleteMessagesEvent extends MessageEvent {

    @Nullable
    private final Id chatId;
    private final boolean scheduled;
    @Nullable
    private final List<Message> deletedMessages;
    private final List<Integer> deleteMessagesIds;

    public DeleteMessagesEvent(MTProtoTelegramClient client, @Nullable Id chatId, boolean scheduled,
                               @Nullable List<Message> deletedMessages, List<Integer> deleteMessagesIds) {
        super(client);
        this.chatId = chatId;
        this.scheduled = scheduled;
        this.deletedMessages = deletedMessages;
        this.deleteMessagesIds = deleteMessagesIds;
    }

    /**
     * Gets id of the chat/channel where messages was deleted, if present.
     *
     * @return The id of the chat/channel where messages was deleted, if present.
     */
    public Optional<Id> getChatId() {
        return Optional.ofNullable(chatId);
    }

    /**
     * Requests to retrieve chat where event was triggered.
     *
     * @return An {@link Mono} emitting on successful completion the {@link Chat chat}.
     */
    public Mono<Chat> getChat() {
        return getChat(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve chat where event was triggered using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link Chat chat}.
     */
    public Mono<Chat> getChat(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(chatId)
                .flatMap(client.withRetrievalStrategy(strategy)::getChatById);
    }

    /**
     * Gets whether deleted messages were scheduled.
     *
     * @return {@code true} if deleted messages were scheduled.
     */
    public boolean isScheduled() {
        return scheduled;
    }

    /**
     * Gets {@link List} with found deleted {@link Message messages}, if present.
     *
     * @return The list with found deleted messages, if present.
     */
    public Optional<List<Message>> getDeletedMessages() {
        return Optional.ofNullable(deletedMessages);
    }

    /**
     * Gets {@link List} of deleted message ids.
     *
     * @return The list of deleted message ids.
     */
    public List<Integer> getDeleteMessagesIds() {
        return deleteMessagesIds;
    }

    @Override
    public String toString() {
        return "DeleteMessagesEvent{" +
                "chatId=" + chatId +
                ", scheduled=" + scheduled +
                ", deletedMessages=" + deletedMessages +
                ", deleteMessagesIds=" + deleteMessagesIds +
                '}';
    }
}
