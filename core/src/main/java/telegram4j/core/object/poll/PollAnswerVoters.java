package telegram4j.core.object.poll;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

/** The poll answer vote statistic. */
public class PollAnswerVoters implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PollAnswerVoters data;

    public PollAnswerVoters(MTProtoTelegramClient client, telegram4j.tl.PollAnswerVoters data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets whether <i>current</i> user chosen this answer.
     *
     * @return Whether <i>current</i> user chosen this answer.
     */
    public boolean isChosen() {
        return data.chosen();
    }

    /**
     * Gets whether <i>current</i> user chosen this answer, and it's correct.
     * Makes sense only for the quiz.
     *
     * @return Whether <i>current</i> user chosen this answer, and it's correct.
     */
    public boolean isCorrect() {
        return data.correct();
    }

    /**
     * Gets the answer parameter that indicates answer.
     *
     * @return The answer parameter in bytes.
     */
    public byte[] getOption() {
        return data.option();
    }

    /**
     * Gets number of votes
     *
     * @return The number of votes.
     */
    public int getVoters() {
        return data.voters();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollAnswerVoters that = (PollAnswerVoters) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PollAnswerVoters{" +
                "data=" + data +
                '}';
    }
}
