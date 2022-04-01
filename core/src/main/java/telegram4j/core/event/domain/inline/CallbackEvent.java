package telegram4j.core.event.domain.inline;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;

import java.util.Optional;

public class CallbackEvent extends BotEvent {
    private final long queryId;
    private final User user;
    private final long chatInstance;
    @Nullable
    private final ByteBuf data;
    @Nullable
    private final String gameShortName;

    public CallbackEvent(MTProtoTelegramClient client, long queryId, User user,
                         long chatInstance, @Nullable ByteBuf data,
                         @Nullable String gameShortName) {
        super(client);
        this.queryId = queryId;
        this.user = user;
        this.chatInstance = chatInstance;
        this.data = data;
        this.gameShortName = gameShortName;
    }

    @Override
    public long getQueryId() {
        return queryId;
    }

    @Override
    public User getUser() {
        return user;
    }

    public long getChatInstance() {
        return chatInstance;
    }

    public Optional<ByteBuf> getData() {
        return Optional.ofNullable(data);
    }

    public Optional<String> getGameShortName() {
        return Optional.ofNullable(gameShortName);
    }

    @Override
    public String toString() {
        return "CallbackEvent{" +
                "queryId=" + queryId +
                ", user=" + user +
                ", chatInstance=" + chatInstance +
                ", data=" + data +
                ", gameShortName='" + gameShortName + '\'' +
                '}';
    }
}
