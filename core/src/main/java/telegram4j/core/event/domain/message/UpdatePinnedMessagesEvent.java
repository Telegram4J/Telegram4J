package telegram4j.core.event.domain.message;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

import java.util.List;

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

    public boolean isPinned() {
        return pinned;
    }

    public Id getChatId() {
        return chatId;
    }

    public List<Integer> getMessageIds() {
        return messageIds;
    }

    public int getPts() {
        return pts;
    }

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
