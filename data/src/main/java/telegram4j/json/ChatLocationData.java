package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableChatLocationData.class)
@JsonDeserialize(as = ImmutableChatLocationData.class)
public interface ChatLocationData {

    static ImmutableChatLocationData.Builder builder() {
        return ImmutableChatLocationData.builder();
    }

    LocationData location();

    String address();
}
