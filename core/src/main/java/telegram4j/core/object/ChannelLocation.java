package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChannelLocation;
import telegram4j.tl.BaseGeoPoint;

import java.util.Objects;
import java.util.Optional;

/**
 * Geolocation representation for supergroups
 *
 * @see <a href="https://telegram.org/blog/contacts-local-groups">Location-Based chats</a>
 */
public class ChannelLocation implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BaseChannelLocation data;

    public ChannelLocation(MTProtoTelegramClient client, BaseChannelLocation data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets geo-point of the address, if present.
     *
     * @return The {@link GeoPoint} of the address, if present.
     */
    public Optional<GeoPoint> getGeoPoint() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geoPoint(), BaseGeoPoint.class))
                .map(d -> new GeoPoint(client, d));
    }

    /**
     * Gets display description of the address.
     *
     * @return The display description of the address.
     */
    public String getAddress() {
        return data.address();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelLocation that = (ChannelLocation) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ChannelLocation{" +
                "data=" + data +
                '}';
    }
}
