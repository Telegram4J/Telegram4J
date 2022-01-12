package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class PhotoPathSize implements PhotoSize {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PhotoPathSize data;

    public PhotoPathSize(MTProtoTelegramClient client, telegram4j.tl.PhotoPathSize data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public String getType() {
        return data.type();
    }

    public byte[] getContent() {
        return data.bytes();
    }
}
