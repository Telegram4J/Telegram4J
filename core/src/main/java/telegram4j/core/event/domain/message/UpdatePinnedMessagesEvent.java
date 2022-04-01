package telegram4j.core.event.domain.message;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.Id;
import telegram4j.tl.ImmutableInputMessageID;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Event when the message(s) were pinned/unpinned from the chat/channel.
 */
public class UpdatePinnedMessagesEvent extends MessageEvent {

    private final boolean pinned;
    private final Id chatId;
    private final List<Integer> messageIds;
    private final int pts;
    private final int ptsCount;

    public UpdatePinnedMessagesEvent(MTProtoTelegramClient client, boolean pinned, Id chatId,
                                     List<Integer> messageIds, int pts, int ptsCount) {
        super(client);
        this.pinned = pinned;
        this.chatId = chatId;
        this.messageIds = messageIds;
        this.pts = pts;
        this.ptsCount = ptsCount;
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
     * Gets ids list of pinned/unpinned messages.
     *
     * @return The ids list of pinned/unpinned messages.
     */
    public List<Integer> getMessageIds() {
        return messageIds;
    }

    /**
     * Retrieves auxiliary messages container with pinned/unpinned messages.
     *
     * @return A {@link Mono} emitting on successful completion messages container with auxiliary information.
     */
    public Mono<AuxiliaryMessages> getMessages() {
        return Mono.defer(() -> {
            var ids = messageIds.stream()
                    .map(ImmutableInputMessageID::of)
                    .collect(Collectors.toList());

            if (chatId.getType() != Id.Type.CHANNEL) {
                return client.getMessagesById(ids);
            }
            return client.getMessagesById(chatId, ids);
        });
    }

    /**
     * Gets pts number after generation.
     *
     * @return The pts number after generation.
     */
    public int getPts() {
        return pts;
    }

    /**
     * Gets number of pts events that were generated.
     *
     * @return The number of pts events.
     */
    public int getPtsCount() {
        return ptsCount;
    }

    @Override
    public String toString() {
        return "UpdatePinnedMessagesEvent{" +
                "pinned=" + pinned +
                ", chatId=" + chatId +
                ", messageIds=" + messageIds +
                ", pts=" + pts +
                ", ptsCount=" + ptsCount +
                '}';
    }
}
