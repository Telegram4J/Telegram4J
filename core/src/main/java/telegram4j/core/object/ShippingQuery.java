package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.ShippingQueryData;

public class ShippingQuery implements TelegramObject {

    private final TelegramClient client;
    private final ShippingQueryData data;

    public ShippingQuery(TelegramClient client, ShippingQueryData data) {
        this.client = client;
        this.data = data;
    }

    public ShippingQueryData getData() {
        return data;
    }

    public String getId() {
        return data.id();
    }

    public User getFromUser() {
        return new User(client, data.fromUser());
    }

    public String getInvoicePayload() {
        return data.invoicePayload();
    }

    public ShippingAddress getShippingAddress() {
        return new ShippingAddress(client, data.shippingAddress());
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingQuery that = (ShippingQuery) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ShippingQuery{" +
                "data=" + data +
                '}';
    }
}
