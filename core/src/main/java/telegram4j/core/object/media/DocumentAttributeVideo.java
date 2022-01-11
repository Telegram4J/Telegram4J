package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Duration;
import java.util.Objects;

public class DocumentAttributeVideo extends BaseDocumentAttribute {

    private final telegram4j.tl.DocumentAttributeVideo data;

    public DocumentAttributeVideo(MTProtoTelegramClient client, telegram4j.tl.DocumentAttributeVideo data) {
        super(client, Type.VIDEO);
        this.data = Objects.requireNonNull(data, "data");
    }

    public boolean isRoundMessage() {
        return data.roundMessage();
    }

    public boolean isSupportsStreaming() {
        return data.supportsStreaming();
    }

    public Duration duration() {
        return Duration.ofSeconds(data.duration());
    }

    public int getWight() {
        return data.w();
    }

    public int getHeight() {
        return data.h();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DocumentAttributeVideo that = (DocumentAttributeVideo) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data);
    }
}
