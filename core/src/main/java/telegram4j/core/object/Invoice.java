package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.InvoiceData;

import java.util.Objects;

public class Invoice implements TelegramObject {

    private final TelegramClient client;
    private final InvoiceData data;

    public Invoice(TelegramClient client, InvoiceData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public InvoiceData getData() {
        return data;
    }

    public String getTitle() {
        return data.title();
    }

    public String getDescription() {
        return data.description();
    }

    public String getStartParameter() {
        return data.startParameter();
    }

    public String getCurrency() {
        return data.currency();
    }

    public int getTotalAmount() {
        return data.totalAmount();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice that = (Invoice) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "Invoice{data=" + data + '}';
    }
}
