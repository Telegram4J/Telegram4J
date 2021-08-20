package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableInlineQueryData.class)
@JsonDeserialize(as = ImmutableInlineQueryData.class)
public interface InlineQueryData {

    static ImmutableInlineQueryData.Builder builder() {
        return ImmutableInlineQueryData.builder();
    }

    String id();

    // NOTE: renamed due matches to generated #from method
    @JsonProperty("from")
    UserData fromUser();

    String query();

    String offset();

    @JsonProperty("chat_type")
    Optional<ChatType> chatType();

    Optional<LocationData> location();
}
