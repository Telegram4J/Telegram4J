package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageActionChatAddUser extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChatAddUser data;

    public MessageActionChatAddUser(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatAddUser data) {
        super(client, Type.CHAT_ADD_USER);
        this.data = Objects.requireNonNull(data, "data");
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
        MessageActionChatAddUser that = (MessageActionChatAddUser) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChatAddUser{" +
                "data=" + data +
                '}';
    }
}
