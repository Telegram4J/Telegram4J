package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableWebhookInfoData.class)
@JsonDeserialize(as = ImmutableWebhookInfoData.class)
public interface WebhookInfoData {

    static ImmutableWebhookInfoData.Builder builder() {
        return ImmutableWebhookInfoData.builder();
    }

    String url();

    @JsonProperty("has_custom_certificate")
    boolean hasCustomCertificate();

    @JsonProperty("pending_update_count")
    int pendingUpdateCount();

    @JsonProperty("pip_address")
    Optional<String> pipAddress();

    @JsonProperty("last_error_date")
    Optional<Integer> lastErrorDate();

    @JsonProperty("last_error_message")
    Optional<String> lastErrorMessage();

    @JsonProperty("max_connections")
    Optional<Integer> maxConnections();

    @JsonProperty("allowed_updates")
    Optional<List<String>> allowedUpdates();
}
