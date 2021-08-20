package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutablePollAnswerData.class)
@JsonDeserialize(as = ImmutablePollAnswerData.class)
public interface PollAnswerData {

    static ImmutablePollAnswerData.Builder builder() {
        return ImmutablePollAnswerData.builder();
    }

    @JsonProperty("poll_id")
    String pollId();

    UserData user();

    @JsonProperty("option_ids")
    List<Integer> optionIds();
}
