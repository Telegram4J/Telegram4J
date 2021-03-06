package telegram4j.core.event.domain.user;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.SendMessageAction;

import java.util.Objects;
import java.util.Optional;

public class UpdateChannelUserTypingEvent extends UserEvent {

    private final Id channelId;
    private final Id fromId;
    private final SendMessageAction action;

    @Nullable
    private final Integer topMessageId;

    public UpdateChannelUserTypingEvent(MTProtoTelegramClient client, Id channelId,
                                        Id fromId, SendMessageAction action,
                                        @Nullable Integer topMessageId) {
        super(client);
        this.channelId = Objects.requireNonNull(channelId, "channelId");
        this.fromId = Objects.requireNonNull(fromId, "fromId");
        this.action = Objects.requireNonNull(action, "action");
        this.topMessageId = topMessageId;
    }

    public Id getChannelId() {
        return channelId;
    }

    public Id getFromId() {
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
                '}';
    }
}
