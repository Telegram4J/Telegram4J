package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableShippingQueryData.class)
@JsonDeserialize(as = ImmutableShippingQueryData.class)
public interface ShippingQueryData {

    static ImmutableShippingQueryData.Builder builder() {
        return ImmutableShippingQueryData.builder();
    }

    String id();

    // NOTE: renamed due matches to generated #from method
    @JsonProperty("from")
    UserData fromUser();

    @JsonProperty("invoice_payload")
    String invoicePayload();

    @JsonProperty("shipping_address")
    ShippingAddressData shippingAddress();
}
