package telegram4j.core.object.media;

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
        this.data = Objects.requireNonNull(data, "data");
    }

    public Optional<GeoPoint> getGeo() {
        return Optional.of(data.geo())
                .map(d -> TlEntityUtil.unmapEmpty(d, BaseGeoPoint.class))
                .map(d -> new GeoPoint(client, d));
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
}
