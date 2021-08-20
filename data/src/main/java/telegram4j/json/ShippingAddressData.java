package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableShippingAddressData.class)
@JsonDeserialize(as = ImmutableShippingAddressData.class)
public interface ShippingAddressData {

    static ImmutableShippingAddressData.Builder builder() {
        return ImmutableShippingAddressData.builder();
    }

    @JsonProperty("country_code")
    String countryCode();

    String state();

    String city();

    @JsonProperty("street_line1")
    String streetLine1();

    @JsonProperty("street_line2")
    String streetLine2();

    @JsonProperty("post_code")
    String postCode();
}
