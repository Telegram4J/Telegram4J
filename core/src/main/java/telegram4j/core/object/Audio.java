package telegram4j.core.object;

import io.netty.buffer.ByteBuf;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseDocumentFields;
import telegram4j.tl.DocumentAttributeAudio;
import telegram4j.tl.InputPeer;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Inferred from {@link BaseDocumentFields#attributes()} type of audio document or voice message. */
public class Audio extends Document {

    private final telegram4j.tl.DocumentAttributeAudio audioData;

    public Audio(MTProtoTelegramClient client, BaseDocumentFields data, String fileName,
                 int messageId, InputPeer peer, DocumentAttributeAudio audioData) {
        super(client, data, fileName, messageId, peer);
        this.audioData = Objects.requireNonNull(audioData);
    }

    /**
     * Gets whether audio document is a voice message.
     *
     * @return Whether audio document is a voice message.
     */
    public boolean isVoice() {
        return audioData.voice();
    }

    /**
     * Gets duration of audio document.
     *
     * @return The duration of document.
     */
    public Duration getDuration() {
        return Duration.ofSeconds(audioData.duration());
    }

    /**
     * Gets name of song, if it's audio document and name present.
     *
     * @return The name of song, if it's audio document and name present.
     */
    public Optional<String> getTitle() {
        return Optional.ofNullable(audioData.title());
    }

    /**
     * Gets name of performer, if it's audio document and name present.
     *
     * @return The name of performer, if it's audio document and name present.
     */
    public Optional<String> getPerformer() {
        return Optional.ofNullable(audioData.performer());
    }

    /**
     * Gets waveform of voice message, if it is and present.
     *
     * @return The waveform of voice message, if it is and present.
     */
    public Optional<ByteBuf> getWaveform() {
        return Optional.ofNullable(audioData.waveform());
    }

    @Override
    public String toString() {
        return "Audio{" +
                "audioData=" + audioData +
                "} " + super.toString();
    }
}
