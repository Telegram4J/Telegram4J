package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.BotInfo;
import telegram4j.core.util.Id;
import telegram4j.tl.BaseChatFull;
import telegram4j.tl.ChatForbidden;
import telegram4j.tl.ChatFull;

import java.util.List;

public final class UnavailableGroupChat extends BaseUnavailableChat implements GroupChatPeer, UnavailableChat {

    private final ChatForbidden data;

    public UnavailableGroupChat(MTProtoTelegramClient client, ChatForbidden data) {
        super(client);
        this.data = data;
    }

    @Override
    public Id getId() {
        return Id.ofChat(data.id());
    }

    @Override
    public Type getType() {
        return Type.GROUP;
    }

    @Override
    public String getName() {
        return data.title();
    }

    @Override
    public String toString() {
        return "UnavailableGroupChat{" +
                "data=" + data +
                '}';
    }
}
