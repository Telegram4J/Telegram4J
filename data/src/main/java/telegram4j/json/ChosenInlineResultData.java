package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableChosenInlineResultData.class)
@JsonDeserialize(as = ImmutableChosenInlineResultData.class)
public interface ChosenInlineResultData {

    static ImmutableChosenInlineResultData.Builder builder() {
        return ImmutableChosenInlineResultData.builder();
    }

    @JsonProperty("result_id")
    String resultId();

    // NOTE: renamed due matches to generated #from method
    @JsonProperty("from")
    UserData fromUser();

    Optional<LocationData> location();

    @JsonProperty("inline_message_id")
    Optional<String> inlineMessageId();

    String query();
}
