package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;

public class PhotoSizeProgressive implements PhotoSize {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PhotoSizeProgressive data;

    public PhotoSizeProgressive(MTProtoTelegramClient client, telegram4j.tl.PhotoSizeProgressive data) {
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

    public int getWight() {
        return data.w();
    }

    public int getHeight() {
        return data.h();
    }

    public List<Integer> getSizes() {
        return data.sizes();
    }
}
