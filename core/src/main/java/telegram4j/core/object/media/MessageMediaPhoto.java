package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Photo;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.InputPeer;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageMediaPhoto extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaPhoto data;
    private final int messageId;
    private final InputPeer peer;

    public MessageMediaPhoto(MTProtoTelegramClient client, telegram4j.tl.MessageMediaPhoto data,
                             int messageId, InputPeer peer) {
        super(client, Type.PHOTO);
        this.data = Objects.requireNonNull(data);
        this.messageId = messageId;
        this.peer = Objects.requireNonNull(peer);
    }


    /**
     * Gets photo of the message, if it hasn't expired by timer.
     *
     * @return The {@link Photo} of the message, if it hasn't expired by timer.
     */
    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.photo(), BasePhoto.class))
                .map(d -> new Photo(client, d, peer, messageId));
    }

    /**
     * Gets {@link Duration} of the photo self-destruction, if present.
     *
     * @return The {@link Duration} of the photo self-destruction, if present.
     */
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
                '}';
    }
}
