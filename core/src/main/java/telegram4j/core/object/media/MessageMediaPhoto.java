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

    public MessageMediaPhoto(MTProtoTelegramClient client, telegram4j.tl.MessageMediaPhoto data) {
        super(client, Type.PHOTO);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Optional<Photo> photo() {
        return Optional.ofNullable(data.photo())
                .map(d -> TlEntityUtil.unmapEmpty(d, BasePhoto.class))
                .map(d -> new Photo(getClient(), d));
    }

    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(data.ttlSeconds()).map(Duration::ofSeconds);
    }
}
