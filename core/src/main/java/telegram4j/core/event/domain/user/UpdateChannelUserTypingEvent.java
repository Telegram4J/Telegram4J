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
    private final Integer topMessageId;

    public UpdateChannelUserTypingEvent(MTProtoTelegramClient client, long channelId,
                                        Peer fromId, SendMessageAction action,
                                        @Nullable Integer topMessageId) {
        super(client);
        this.channelId = channelId;
        this.fromId = TlEntityUtil.peerId(fromId);
        this.action = action;
        this.topMessageId = topMessageId;
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

    public Optional<Integer> getTopMessageId() {
        return Optional.ofNullable(topMessageId);
    }

    @Override
    public String toString() {
        return "UpdateChannelUserTypingEvent{" +
                "channelId=" + channelId +
                ", fromId=" + fromId +
                ", action=" + action +
                ", topMessageId=" + topMessageId +
                "} " + super.toString();
    }
}
