package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.GeoPoint;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseGeoPoint;

import java.util.Objects;
import java.util.Optional;

public class MessageMediaVenue extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaVenue data;

    public MessageMediaVenue(MTProtoTelegramClient client, telegram4j.tl.MessageMediaVenue data) {
        super(client, Type.VENUE);
        this.data = Objects.requireNonNull(data);
    }

    public Optional<GeoPoint> getGeo() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geo(), BaseGeoPoint.class)).map(GeoPoint::new);
    }

    public String getTitle() {
        return data.title();
    }

    public String getAddress() {
        return data.address();
    }

    public String getProvider() {
        return data.provider();
    }

    public String getVenueId() {
        return data.venueId();
    }

    public String getVenueType() {
        return data.venueType();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaVenue that = (MessageMediaVenue) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaVenue{" +
                "data=" + data +
                '}';
    }
}
