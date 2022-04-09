package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.InputGroupCall;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageActionInviteToGroupCall extends BaseMessageAction {

    private final telegram4j.tl.MessageActionInviteToGroupCall data;

    public MessageActionInviteToGroupCall(MTProtoTelegramClient client, telegram4j.tl.MessageActionInviteToGroupCall data) {
        super(client, Type.INVITE_TO_GROUP_CALL);
        this.data = Objects.requireNonNull(data, "data");
    }

    public InputGroupCall getCall() {
        return data.call();
    }

    public List<Id> getUserIds() {
        return data.users().stream()
                .map(l -> Id.ofUser(l, null))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionInviteToGroupCall that = (MessageActionInviteToGroupCall) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionInviteToGroupCall{" +
                "data=" + data +
                '}';
    }
}
