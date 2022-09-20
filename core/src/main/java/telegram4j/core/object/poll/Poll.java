package telegram4j.core.object.poll;

import reactor.util.annotation.Nullable;
import telegram4j.tl.PollAnswer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** <a href="https://core.telegram.org/api/poll">Poll</a> object, which represents active or closed poll. */
public class Poll {

    private final telegram4j.tl.Poll data;

    public Poll(telegram4j.tl.Poll data) {
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets the server-assigned poll id.
     *
     * @return The id of poll.
     */
    public long getId() {
        return data.id();
    }

    /**
     * Gets whether the poll is closed.
     *
     * @return Whether the poll is closed.
     */
    public boolean isClosed() {
        return data.closed();
    }

    /**
     * Gets whether cast votes are publicly visible to all users (non-anonymous poll)
     *
     * @return Whether cast votes are publicly visible to all users.
     */
    public boolean isPublicVoters() {
        return data.publicVoters();
    }

    /**
     * Gets whether multiple options can be chosen as answer.
     *
     * @return Whether multiple options can be chosen as answer.
     */
    public boolean isMultipleChoice() {
        return data.multipleChoice();
    }

    /**
     * Gets whether poll is the quiz.
     *
     * @return Whether poll is the quiz.
     */
    public boolean isQuiz() {
        return data.quiz();
    }

    /**
     * Gets the non-formatted poll question.
     *
     * @return The poll question.
     */
    public String getQuestion() {
        return data.question();
    }

    /**
     * Gets list of answers (2-10) of poll.
     *
     * @return The list of answers.
     */
    public List<PollAnswer> getAnswers() {
        return data.answers();
    }

    /**
     * Gets the duration during which the poll is active, if present.
     *
     * @return The duration during which the poll is active, if present.
     */
    public Optional<Duration> getClosePeriod() {
        return Optional.ofNullable(data.closePeriod()).map(Duration::ofSeconds);
    }

    /**
     * Gets the timestamp, when poll will be closed, if present.
     *
     * @return The timestamp of poll close, if present.
     */
    public Optional<Instant> getCloseTimestamp() {
        return Optional.ofNullable(data.closeDate()).map(Instant::ofEpochSecond);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Poll poll = (Poll) o;
        return data.equals(poll.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Poll{data=" + data + '}';
    }
}
