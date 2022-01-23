package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Photo;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BasePhoto;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageMediaPhoto extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaPhoto data;

    public MessageMediaPhoto(MTProtoTelegramClient client, telegram4j.tl.MessageMediaPhoto data, int messageId) {
        super(client, Type.PHOTO, messageId);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Optional<Photo> photo() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.photo(), BasePhoto.class))
                .map(d -> new Photo(getClient(), d, messageId));
    }

    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(data.ttlSeconds()).map(Duration::ofSeconds);
    }
}
