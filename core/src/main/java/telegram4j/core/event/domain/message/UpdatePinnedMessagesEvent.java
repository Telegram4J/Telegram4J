package telegram4j.core.event.domain.message;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.ImmutableInputMessageID;

import java.util.List;
import java.util.stream.Collectors;

/** Event when the message(s) were pinned/unpinned from the chat/channel. */
public class UpdatePinnedMessagesEvent extends MessageEvent {

    private final boolean pinned;
    private final Id chatId;
    private final List<Integer> messageIds;

    public UpdatePinnedMessagesEvent(MTProtoTelegramClient client, boolean pinned, Id chatId, List<Integer> messageIds) {
        super(client);
        this.pinned = pinned;
        this.chatId = chatId;
        this.messageIds = messageIds;
    }

    /**
     * Gets whether this event contains pinned messages.
     *
     * @return Gets whether event contains pinned messages.
     */
    public boolean isPinned() {
        return pinned;
    }

    /**
     * Gets id of chat/channel where that messages were pinned/unpinned.
     *
     * @return The id of related chat/channel.
     */
    public Id getChatId() {
        return chatId;
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
        return client.withRetrievalStrategy(strategy).getChatById(chatId);
    }

    /**
     * Gets ids list of pinned/unpinned messages.
     *
     * @return The ids list of pinned/unpinned messages.
     */
    public List<Integer> getMessageIds() {
        return messageIds;
    }

    /**
     * Retrieves messages container with pinned/unpinned messages.
     *
     * @return A {@link Mono} emitting on successful completion messages container with auxiliary information.
     */
    public Mono<AuxiliaryMessages> getMessages() {
        return getMessages(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Retrieves messages container with pinned/unpinned messages using specified strategy.
     *
     * @param strategy The strategy to apply.
     * @return A {@link Mono} emitting on successful completion messages container with auxiliary information.
     */
    public Mono<AuxiliaryMessages> getMessages(EntityRetrievalStrategy strategy) {
        return Mono.defer(() -> client.withRetrievalStrategy(strategy)
                .getMessagesById(chatId, messageIds.stream()
                        .map(ImmutableInputMessageID::of)
                        .collect(Collectors.toUnmodifiableList())));
    }

    @Override
    public String toString() {
        return "UpdatePinnedMessagesEvent{" +
                "pinned=" + pinned +
                ", chatId=" + chatId +
                ", messageIds=" + messageIds +
                '}';
    }
}
