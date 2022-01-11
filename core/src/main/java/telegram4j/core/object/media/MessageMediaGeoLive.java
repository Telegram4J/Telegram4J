package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.GeoPoint;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseGeoPoint;

import java.util.Objects;
import java.util.Optional;

public class MessageMediaGeoLive extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaGeoLive data;

    public MessageMediaGeoLive(MTProtoTelegramClient client, telegram4j.tl.MessageMediaGeoLive data) {
        super(client, Type.GEO_LIVE);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Optional<GeoPoint> getGeo() {
        return Optional.of(data.geo())
                .map(d -> TlEntityUtil.unmapEmpty(d, BaseGeoPoint.class))
                .map(d -> new GeoPoint(client, d));
    }

    public Optional<Integer> getHeading() {
        return Optional.ofNullable(data.heading());
    }

    public int getPeriod() {
        return data.period();
    }

    public Optional<Integer> getProximityNotificationRadius() {
        return Optional.ofNullable(data.proximityNotificationRadius());
    }
}
