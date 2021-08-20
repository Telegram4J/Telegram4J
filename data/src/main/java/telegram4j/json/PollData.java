package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePollData.class)
@JsonDeserialize(as = ImmutablePollData.class)
public interface PollData {

    static ImmutablePollData.Builder builder() {
        return ImmutablePollData.builder();
    }

    String id();

    String question();

    List<PollOptionData> options();

    @JsonProperty("total_voter_count")
    int totalVoterCount();

    @JsonProperty("is_closed")
    boolean isClosed();

    @JsonProperty("is_anonymous")
    boolean isAnonymous();

    PollType type();

    @JsonProperty("allows_multiple_answers")
    boolean allowsMultipleAnswers();

    @JsonProperty("correct_option_id")
    Optional<Integer> correctOptionId();

    Optional<String> explanation();

    @JsonProperty("explanation_entities")
    Optional<List<MessageEntityData>> explanationEntities();

    @JsonProperty("open_period")
    Optional<Integer> openPeriod();

    @JsonProperty("close_date")
    Optional<Integer> closeDate();
}
