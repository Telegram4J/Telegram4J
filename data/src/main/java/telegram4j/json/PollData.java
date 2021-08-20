package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
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
