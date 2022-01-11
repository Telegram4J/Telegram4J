package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageMediaGame extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaGame data;

    public MessageMediaGame(MTProtoTelegramClient client, telegram4j.tl.MessageMediaGame data) {
        super(client, Type.GAME);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Game getGame() {
        return new Game(client, data.game());
    }
}
