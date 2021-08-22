package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.SuccessfulPaymentData;

import java.util.Objects;
import java.util.Optional;

public class SuccessfulPayment implements TelegramObject {

    private final TelegramClient client;
    private final SuccessfulPaymentData data;

    public SuccessfulPayment(TelegramClient client, SuccessfulPaymentData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public SuccessfulPaymentData getData() {
        return data;
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

    public Optional<String> getSnippingOptionId() {
        return data.snippingOptionId();
    }

    public Optional<OrderInfo> getOrderInfo() {
        return data.orderInfo().map(data -> new OrderInfo(client, data));
    }

    public String getTelegramPaymentChargeId() {
        return data.telegramPaymentChargeId();
    }

    public String getProviderPaymentChargeId() {
        return data.providerPaymentChargeId();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SuccessfulPayment that = (SuccessfulPayment) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "SuccessfulPayment{data=" + data + '}';
    }
}
