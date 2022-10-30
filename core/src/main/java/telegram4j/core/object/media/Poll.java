package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.util.BitFlag;
import telegram4j.tl.PollAnswer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

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
     * Gets set of poll flags.
     *
     * @return The set of poll flags.
     */
    public Set<Flag> getFlags() {
        return Flag.of(data);
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

    /** An enumeration of {@link telegram4j.tl.Poll} bit-flags. */
    public enum Flag implements BitFlag {
        /** Whether poll is closed. */
        CLOSED(telegram4j.tl.Poll.CLOSED_POS),

        /** Whether cast votes are publicly visible to all users (non-anonymous poll). */
        PUBLIC_VOTERS(telegram4j.tl.Poll.PUBLIC_VOTERS_POS),

        MULTIPLE_CHOICE(telegram4j.tl.Poll.MULTIPLE_CHOICE_POS),

        /** Whether poll is quiz. This flag can't be set with {@link #MULTIPLE_CHOICE}. */
        QUIZ(telegram4j.tl.Poll.QUIZ_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        public static Set<Flag> of(telegram4j.tl.Poll data) {
            var set = EnumSet.allOf(Flag.class);
            int flags = data.flags();
            set.removeIf(f -> (flags & f.mask()) == 0);
            return set;
        }

        @Override
        public byte position() {
            return position;
        }
    }
}
