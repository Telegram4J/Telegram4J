package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

@Value.Immutable
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
