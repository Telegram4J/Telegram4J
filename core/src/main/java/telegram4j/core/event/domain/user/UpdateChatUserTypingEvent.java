package telegram4j.core.event.domain.user;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.Peer;
import telegram4j.tl.SendMessageAction;

public class UpdateChatUserTypingEvent extends UserEvent {
    private final long chatId;
    private final SendMessageAction action;
    private final long fromId;

    public UpdateChatUserTypingEvent(MTProtoTelegramClient client, long chatId, Peer fromId, SendMessageAction action) {
        super(client);
        this.chatId = chatId;
        this.fromId = TlEntityUtil.peerId(fromId);
        this.action = action;
    }

    public long getChatId() {
        return chatId;
    }

    public long getFromId() {
        return fromId;
    }

    public SendMessageAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "UpdateChatUserTypingEvent{" +
                "chatId=" + chatId +
                ", action=" + action +
                ", fromId=" + fromId +
                "} " + super.toString();
    }
}
