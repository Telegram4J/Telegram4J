package telegram4j.core.object.media;

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
}
