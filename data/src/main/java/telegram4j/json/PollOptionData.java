package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutablePollOptionData.class)
@JsonDeserialize(as = ImmutablePollOptionData.class)
public interface PollOptionData {

    static ImmutablePollOptionData.Builder builder() {
        return ImmutablePollOptionData.builder();
    }

    String text();

    @JsonProperty("voter_count")
    int voteCount();
}
