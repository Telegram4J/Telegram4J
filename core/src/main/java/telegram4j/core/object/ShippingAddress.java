package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.ShippingAddressData;

import java.util.Objects;

public class ShippingAddress implements TelegramObject {

    private final TelegramClient client;
    private final ShippingAddressData data;

    public ShippingAddress(TelegramClient client, ShippingAddressData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public ShippingAddressData getData() {
        return data;
    }

    public String getCountryCode() {
        return data.countryCode();
    }

    public String getState() {
        return data.state();
    }

    public String getCity() {
        return data.city();
    }

    public String getStreetLine1() {
        return data.streetLine1();
    }

    public String getStreetLine2() {
        return data.streetLine2();
    }

    public String getPostCode() {
        return data.postCode();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingAddress that = (ShippingAddress) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ShippingAddress{data=" + data + '}';
    }
}
