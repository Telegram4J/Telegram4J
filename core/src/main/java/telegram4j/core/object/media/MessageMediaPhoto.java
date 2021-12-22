package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageMediaPhoto extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaPhoto data;

    public MessageMediaPhoto(MTProtoTelegramClient client, telegram4j.tl.MessageMediaPhoto data) {
        super(client, Type.PHOTO);
        this.data = Objects.requireNonNull(data, "data");
    }

    // @Nullable
    // public Photo photo() {
    //     return data.photo();
    // }

    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(data.ttlSeconds()).map(Duration::ofSeconds);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaPhoto that = (MessageMediaPhoto) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaPhoto{" +
                "data=" + data +
                "} " + super.toString();
    }
}
