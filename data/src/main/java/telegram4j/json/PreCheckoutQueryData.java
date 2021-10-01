package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePreCheckoutQueryData.class)
@JsonDeserialize(as = ImmutablePreCheckoutQueryData.class)
public interface PreCheckoutQueryData {

    static ImmutablePreCheckoutQueryData.Builder builder() {
        return ImmutablePreCheckoutQueryData.builder();
    }

    String id();

    // NOTE: renamed due matches to generated #from method
    @JsonProperty("from")
    UserData fromUser();

    String currency();

    @JsonProperty("total_amount")
    int totalAmount();

    @JsonProperty("invoice_payload")
    String invoicePayload();

    @JsonProperty("shipping_option_id")
    Optional<String> shippingOptionId();

    @JsonProperty("order_info")
    Optional<OrderInfoData> orderInfo();
}
