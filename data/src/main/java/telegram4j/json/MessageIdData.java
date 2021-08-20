package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageIdData.class)
@JsonDeserialize(as = ImmutableMessageIdData.class)
public interface MessageIdData {

    static ImmutableMessageIdData.Builder builder() {
        return ImmutableMessageIdData.builder();
    }

    @JsonProperty("message_id")
    long messageId();
}
