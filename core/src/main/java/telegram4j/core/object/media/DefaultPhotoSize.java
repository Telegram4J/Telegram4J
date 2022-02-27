package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class DefaultPhotoSize implements PhotoSize {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BasePhotoSize data;

    public DefaultPhotoSize(MTProtoTelegramClient client, telegram4j.tl.BasePhotoSize data) {
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

    public int getSize() {
        return data.size();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultPhotoSize that = (DefaultPhotoSize) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "DefaultPhotoSize{" +
                "data=" + data +
                '}';
    }
}
