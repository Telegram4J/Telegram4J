package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableInvoiceData.class)
@JsonDeserialize(as = ImmutableInvoiceData.class)
public interface InvoiceData {

    static ImmutableInvoiceData.Builder builder() {
        return ImmutableInvoiceData.builder();
    }

    String title();

    String description();

    @JsonProperty("start_parameter")
    String startParameter();

    String currency();

    @JsonProperty("total_amount")
    int totalAmount();
}
