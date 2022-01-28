package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public class MessageActionPaymentSent extends BaseMessageAction {

    private final telegram4j.tl.MessageActionPaymentSent data;

    public MessageActionPaymentSent(MTProtoTelegramClient client, telegram4j.tl.MessageActionPaymentSent data) {
        super(client, Type.PAYMENT_SENT);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getCurrency() {
        return data.currency();
    }

    public long getTotalAmount() {
        return data.totalAmount();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionPaymentSent that = (MessageActionPaymentSent) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionPaymentSent{" +
                "data=" + data +
                '}';
    }
}
