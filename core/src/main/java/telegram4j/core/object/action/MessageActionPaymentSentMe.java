package telegram4j.core.object.action;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.PaymentCharge;
import telegram4j.tl.PaymentRequestedInfo;

import java.util.Objects;
import java.util.Optional;

public class MessageActionPaymentSentMe extends BaseMessageAction {

    private final telegram4j.tl.MessageActionPaymentSentMe data;

    public MessageActionPaymentSentMe(MTProtoTelegramClient client, telegram4j.tl.MessageActionPaymentSentMe data) {
        super(client, Type.PAYMENT_SENT);
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getCurrency() {
        return data.currency();
    }

    public long getTotalAmount() {
        return data.totalAmount();
    }

    public ByteBuf getPayload() {
        return data.payload();
    }

    public Optional<PaymentRequestedInfo> getInfo() {
        return Optional.ofNullable(data.info());
    }

    public Optional<String> getShippingOptionId() {
        return Optional.ofNullable(data.shippingOptionId());
    }

    public PaymentCharge getCharge() {
        return data.charge();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionPaymentSentMe that = (MessageActionPaymentSentMe) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionPaymentSentMe{" +
                "data=" + data +
                '}';
    }
}
