package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;

import java.util.Objects;

public class MessageActionChatJoinedByLink extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChatJoinedByLink data;

    public MessageActionChatJoinedByLink(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatJoinedByLink data) {
        super(client, Type.CHAT_JOINED_BY_LINK);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Id getInviterId() {
        return Id.ofUser(data.inviterId(), null);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionChatJoinedByLink that = (MessageActionChatJoinedByLink) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChatJoinedByLink{" +
                "data=" + data +
                '}';
    }
}
