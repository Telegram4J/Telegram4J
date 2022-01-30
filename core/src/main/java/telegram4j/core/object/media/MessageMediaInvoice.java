package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;
import java.util.Optional;

public class MessageMediaInvoice extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaInvoice data;

    public MessageMediaInvoice(MTProtoTelegramClient client, telegram4j.tl.MessageMediaInvoice data) {
        super(client, Type.INVOICE);
        this.data = Objects.requireNonNull(data, "data");
    }

    public boolean isShippingAddressRequested() {
        return data.shippingAddressRequested();
    }

    public boolean isTest() {
        return data.test();
    }

    public String getTitle() {
        return data.title();
    }

    public String getDescription() {
        return data.description();
    }

    public Optional<WebDocument> getPhoto() {
        return Optional.ofNullable(data.photo()).map(d -> new WebDocument(client, d));
    }

    public Optional<Integer> getReceiptMessageId() {
        return Optional.ofNullable(data.receiptMsgId());
    }

    public String getCurrency() {
        return data.currency();
    }

    public long getTotalAmount() {
        return data.totalAmount();
    }

    public String getStartParam() {
        return data.startParam();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaInvoice that = (MessageMediaInvoice) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaInvoice{" +
                "data=" + data +
                '}';
    }
}
