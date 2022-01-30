package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class DocumentAttributeAudio extends BaseDocumentAttribute {

    private final telegram4j.tl.DocumentAttributeAudio data;

    public DocumentAttributeAudio(MTProtoTelegramClient client, telegram4j.tl.DocumentAttributeAudio data) {
        super(client, Type.AUDIO);
        this.data = Objects.requireNonNull(data, "data");
    }

    public boolean isVoice() {
        return data.voice();
    }

    public Duration getDuration() {
        return Duration.ofSeconds(data.duration());
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(data.title());
    }

    public Optional<String> getPerformer() {
        return Optional.ofNullable(data.performer());
    }

    public Optional<byte[]> getWaveform() {
        return Optional.ofNullable(data.waveform());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentAttributeAudio that = (DocumentAttributeAudio) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "DocumentAttributeAudio{" +
                "data=" + data +
                '}';
    }
}
