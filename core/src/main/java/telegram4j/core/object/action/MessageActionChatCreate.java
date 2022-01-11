package telegram4j.core.object.action;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageActionChatCreate extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChatCreate data;

    public MessageActionChatCreate(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatCreate data) {
        super(client, Type.CHAT_CREATE);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getTitle() {
        return data.title();
    }

    public List<Id> getUserIds() {
        return data.users().stream()
                .map(l -> Id.ofUser(l, null))
                .collect(Collectors.toList());
    }
}
