package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
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
