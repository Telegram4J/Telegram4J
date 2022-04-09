package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;

import java.util.Objects;

public class MessageActionGeoProximityReached extends BaseMessageAction {

    private final telegram4j.tl.MessageActionGeoProximityReached data;

    public MessageActionGeoProximityReached(MTProtoTelegramClient client, telegram4j.tl.MessageActionGeoProximityReached data) {
        super(client, Type.GEO_PROXIMITY_REACHED);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Id getFromPeerId() {
        return Id.of(data.fromId());
    }

    public Id getDestinationPeerId() {
        return Id.of(data.toId());
    }

    public int getDistance() {
        return data.distance();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionGeoProximityReached that = (MessageActionGeoProximityReached) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionGeoProximityReached{" +
                "data=" + data +
                '}';
    }
}
