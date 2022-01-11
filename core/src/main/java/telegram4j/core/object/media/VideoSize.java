package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;
import java.util.Optional;

public class VideoSize implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.VideoSize data;

    public VideoSize(MTProtoTelegramClient client, telegram4j.tl.VideoSize data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

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

    public Optional<Double> getVideoStartTimestamp() {
        return Optional.ofNullable(data.videoStartTs());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoSize videoSize = (VideoSize) o;
        return data.equals(videoSize.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
