package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableLoginUrlData.class)
@JsonDeserialize(as = ImmutableLoginUrlData.class)
public interface LoginUrlData {

    static ImmutableLoginUrlData.Builder builder() {
        return ImmutableLoginUrlData.builder();
    }

    String url();

    @JsonProperty("forward_text")
    Optional<String> forwardText();

    @JsonProperty("bot_username")
    Optional<String> botUsername();

    @JsonProperty("request_write_access")
    boolean requestWriteAccess();
}
