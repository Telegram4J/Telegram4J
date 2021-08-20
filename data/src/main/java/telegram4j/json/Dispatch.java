package telegram4j.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableDispatch.class)
@JsonDeserialize(as = ImmutableDispatch.class)
public interface Dispatch {

    static ImmutableDispatch.Builder builder() {
        return ImmutableDispatch.builder();
    }

    boolean ok();

    JsonNode result();
}
