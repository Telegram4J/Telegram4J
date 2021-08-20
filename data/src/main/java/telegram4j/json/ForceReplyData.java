package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableForceReplyData.class)
@JsonDeserialize(as = ImmutableForceReplyData.class)
public interface ForceReplyData {

    static ImmutableForceReplyData.Builder builder() {
        return ImmutableForceReplyData.builder();
    }

    @JsonProperty("force_reply")
    boolean forceReply();

    @JsonProperty("input_field_placeholder")
    Optional<String> inputFieldPlaceholder();

    Optional<Boolean> selective();
}
