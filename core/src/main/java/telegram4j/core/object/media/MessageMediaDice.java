package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageMediaDice extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaDice data;

    public MessageMediaDice(MTProtoTelegramClient client, telegram4j.tl.MessageMediaDice data) {
        super(client, Type.DICE);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets value of dice.
     *
     * @see <a href="https://core.telegram.org/api/dice">Dice</a>
     * @return The value of dice.
     */
    public int getValue() {
        return data.value();
    }

    /**
     * Gets dice unicode emoji.
     *
     * @return The dice unicode emoji.
     */
    public String getEmoticon() {
        return data.emoticon();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaDice that = (MessageMediaDice) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaDice{" +
                "data=" + data +
                '}';
    }
}
