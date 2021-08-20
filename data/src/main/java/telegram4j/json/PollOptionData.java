package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

@Value.Immutable
public interface PollOptionData {

    static ImmutablePollOptionData.Builder builder() {
        return ImmutablePollOptionData.builder();
    }

    String text();

    @JsonProperty("voter_count")
    int voteCount();
}
