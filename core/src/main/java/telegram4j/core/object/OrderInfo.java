package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.OrderInfoData;

import java.util.Objects;
import java.util.Optional;

public class OrderInfo implements TelegramObject {

    private final TelegramClient client;
    private final OrderInfoData data;

    public OrderInfo(TelegramClient client, OrderInfoData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public OrderInfoData getData() {
        return data;
    }

    public Optional<String> getName() {
        return data.name();
    }

    public Optional<String> getPhoneNumber() {
        return data.phoneNumber();
    }

    public Optional<String> getEmail() {
        return data.email();
    }

    public Optional<ShippingAddress> shippingAddress() {
        return data.shippingAddress().map(data -> new ShippingAddress(client, data));
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderInfo that = (OrderInfo) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "OrderInfo{data=" + data + '}';
    }
}
