package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

@Value.Immutable
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
