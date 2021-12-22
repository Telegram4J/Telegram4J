package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChannelLocation;
import telegram4j.tl.BaseGeoPoint;

import java.util.Objects;
import java.util.Optional;

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

    public Optional<GeoPoint> getGeoPoint() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geoPoint(), BaseGeoPoint.class))
                .map(d -> new GeoPoint(client, d));
    }

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
