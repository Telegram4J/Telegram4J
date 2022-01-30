package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageMediaDice extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaDice data;

    public MessageMediaDice(MTProtoTelegramClient client, telegram4j.tl.MessageMediaDice data) {
        super(client, Type.DICE);
        this.data = Objects.requireNonNull(data, "data");
    }

    public int getValue() {
        return data.value();
    }

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
