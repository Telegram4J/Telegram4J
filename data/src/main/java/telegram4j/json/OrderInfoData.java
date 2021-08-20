package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable(singleton = true) // because all fields are optional
@JsonSerialize(as = ImmutableOrderInfoData.class)
@JsonDeserialize(as = ImmutableOrderInfoData.class)
public interface OrderInfoData {

    static ImmutableOrderInfoData.Builder builder() {
        return ImmutableOrderInfoData.builder();
    }

    Optional<String> name();

    @JsonProperty("phone_number")
    Optional<String> phoneNumber();

    Optional<String> email();

    @JsonProperty("shipping_address")
    Optional<ShippingAddressData> shippingAddress();
}
