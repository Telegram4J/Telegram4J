package telegram4j.core.event.domain.user;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.Peer;
import telegram4j.tl.SendMessageAction;

import java.util.Optional;

public class UpdateChannelUserTypingEvent extends UserEvent {

    private final long channelId;
    private final long fromId;
    private final SendMessageAction action;

    @Nullable
    private final Integer topMsgId;

    public UpdateChannelUserTypingEvent(MTProtoTelegramClient client, long user_id, Peer fromId, SendMessageAction action, @Nullable Integer topMsgId) {
        super(client);
        this.channelId = user_id;
        this.fromId = TlEntityUtil.peerId(fromId);
        this.action = action;
        this.topMsgId = topMsgId;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getFromId() {
        return fromId;
    }

    public SendMessageAction getAction() {
        return action;
    }

    public Optional<Integer> getTopMsgId() {
        return Optional.ofNullable(topMsgId);
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "channel_id=" + channelId +
                ", from_id=" + fromId +
                ", action=" + action +
                ", top_msg_id=" + topMsgId +
                "} " + super.toString();
    }
}
