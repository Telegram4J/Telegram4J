package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.PreCheckoutQueryData;

import java.util.Optional;

public class PreCheckoutQuery implements TelegramObject {

    private final TelegramClient client;
    private final PreCheckoutQueryData data;

    public PreCheckoutQuery(TelegramClient client, PreCheckoutQueryData data) {
        this.client = client;
        this.data = data;
    }

    public PreCheckoutQueryData getData() {
        return data;
    }

    public String getId() {
        return data.id();
    }

    public User getFromUser() {
        return new User(client, data.fromUser());
    }

    public String getCurrency() {
        return data.currency();
    }

    public int getTotalAmount() {
        return data.totalAmount();
    }

    public String getInvoicePayload() {
        return data.invoicePayload();
    }

    public Optional<String> getShippingOptionId() {
        return data.shippingOptionId();
    }

    public Optional<OrderInfo> getOrderInfo() {
        return data.orderInfo().map(data -> new OrderInfo(client, data));
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreCheckoutQuery that = (PreCheckoutQuery) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PreCheckoutQuery{" +
                "data=" + data +
                '}';
    }
}
