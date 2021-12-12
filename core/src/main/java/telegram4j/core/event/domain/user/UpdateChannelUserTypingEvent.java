package telegram4j.core.event.domain.user;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.Peer;
import telegram4j.tl.SendMessageAction;

import java.util.Optional;

public class UpdateChannelUserTypingEvent extends UserEvent {

    private final long channel_id;
    private final long from_id;
    private final SendMessageAction action;

    @Nullable
    private final Integer top_msg_id;

    public UpdateChannelUserTypingEvent(MTProtoTelegramClient client, long user_id, Peer from_id, SendMessageAction action, @Nullable Integer top_msg_id) {
        super(client);
        this.channel_id = user_id;
        this.from_id = TlEntityUtil.peerId(from_id);
        this.action = action;
        this.top_msg_id = top_msg_id;
    }

    public long getChannel_id() {
        return channel_id;
    }

    public long getFrom_id() {
        return from_id;
    }

    public SendMessageAction getAction() {
        return action;
    }

    public Optional<Integer> getTop_msg_id() {
        return Optional.ofNullable(top_msg_id);
    }

    @Override
    public String toString() {
        return "UpdateUserTypingEvent{" +
                "channel_id=" + channel_id +
                ", from_id=" + from_id +
                ", action=" + action +
                ", top_msg_id=" + top_msg_id +
                "} " + super.toString();
    }
}
