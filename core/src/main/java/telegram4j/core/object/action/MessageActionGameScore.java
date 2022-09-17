package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageActionGameScore extends BaseMessageAction {

    private final telegram4j.tl.MessageActionGameScore data;

    public MessageActionGameScore(MTProtoTelegramClient client, telegram4j.tl.MessageActionGameScore data) {
        super(client, Type.GAME_SCORE);
        this.data = Objects.requireNonNull(data);
    }

    public long getGameId() {
        return data.gameId();
    }

    public int getScore() {
        return data.score();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionGameScore that = (MessageActionGameScore) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionGameScore{" +
                "data=" + data +
                '}';
    }
}
