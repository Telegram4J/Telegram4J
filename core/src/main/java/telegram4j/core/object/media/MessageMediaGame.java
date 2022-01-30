package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputPeer;

import java.util.Objects;

public class MessageMediaGame extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaGame data;
    private final int messageId;
    private final InputPeer peer;

    public MessageMediaGame(MTProtoTelegramClient client, telegram4j.tl.MessageMediaGame data,
                            int messageId, InputPeer peer) {
        super(client, Type.GAME);
        this.data = Objects.requireNonNull(data, "data");
        this.messageId = messageId;
        this.peer = Objects.requireNonNull(peer, "peer");
    }

    public Game getGame() {
        return new Game(client, data.game(), messageId, peer);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaGame that = (MessageMediaGame) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaGame{" +
                "data=" + data +
                '}';
    }
}
