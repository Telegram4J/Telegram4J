package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.DocumentAttributeAudio;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class Audio extends Document {

    private final telegram4j.tl.DocumentAttributeAudio audioData;

    public Audio(MTProtoTelegramClient client, BaseDocument data, String fileName,
                 int messageId, DocumentAttributeAudio audioData) {
        super(client, data, fileName, messageId);
        this.audioData = Objects.requireNonNull(audioData, "audioData");
    }

    public boolean isVoice() {
        return audioData.voice();
    }

    public Duration getDuration() {
        return Duration.ofSeconds(audioData.duration());
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(audioData.title());
    }

    public Optional<String> getPerformer() {
        return Optional.ofNullable(audioData.performer());
    }

    public Optional<byte[]> getWaveform() {
        return Optional.ofNullable(audioData.waveform());
    }

    @Override
    public String toString() {
        return "Audio{" +
                "audioData=" + audioData +
                "} " + super.toString();
    }
}
