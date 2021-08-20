package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableSuccessfulPaymentData.class)
@JsonDeserialize(as = ImmutableSuccessfulPaymentData.class)
public interface SuccessfulPaymentData {

    static ImmutableSuccessfulPaymentData.Builder builder() {
        return ImmutableSuccessfulPaymentData.builder();
    }

    String currency();

    @JsonProperty("total_amount")
    int totalAmount();

    @JsonProperty("invoice_payload")
    String invoicePayload();

    @JsonProperty("snipping_option_id")
    Optional<String> snippingOptionId();

    @JsonProperty("order_info")
    Optional<OrderInfoData> orderInfo();

    @JsonProperty("telegram_payment_charge_id")
    String telegramPaymentChargeId();

    @JsonProperty("provider_payment_charge_id")
    String providerPaymentChargeId();
}
