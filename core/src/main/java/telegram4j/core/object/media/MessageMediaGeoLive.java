package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
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
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geo(), BaseGeoPoint.class))
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

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaGeoLive that = (MessageMediaGeoLive) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaGeoLive{" +
                "data=" + data +
                '}';
    }
}
