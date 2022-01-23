package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.GeoPoint;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseGeoPoint;

import java.util.Objects;
import java.util.Optional;

public class MessageMediaGeo extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaGeo data;

    public MessageMediaGeo(MTProtoTelegramClient client, telegram4j.tl.MessageMediaGeo data, int messageId) {
        super(client, Type.GEO, messageId);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Optional<GeoPoint> getGeo() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geo(), BaseGeoPoint.class))
                .map(d -> new GeoPoint(client, d));
    }
}
