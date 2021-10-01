package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.ShippingQuery;

public class ShippingQueryEvent extends Event {
    private final ShippingQuery shippingQuery;

    public ShippingQueryEvent(TelegramClient client, ShippingQuery shippingQuery) {
        super(client);
        this.shippingQuery = shippingQuery;
    }

    public ShippingQuery getShippingQuery() {
        return shippingQuery;
    }

    @Override
    public String toString() {
        return "ShippingQueryEvent{" +
                "shippingQuery=" + shippingQuery +
                "} " + super.toString();
    }
}
