package telegram4j.core.object.poll;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PollResults implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PollResults data;

    public PollResults(MTProtoTelegramClient client, telegram4j.tl.PollResults data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets whether poll results is minimal and if {@literal true}, option
     * chosen by the <i>current</i> user is not included.
     *
     * @return Whether poll results in minimal.
     */
    public boolean isMin() {
        return data.min();
    }

    /**
     * Gets list of poll answer results, if present.
     *
     * @return List of poll answer results, if present.
     */
    public Optional<List<PollAnswerVoters>> getResults() {
        return Optional.ofNullable(data.results())
                .map(l -> l.stream()
                        .map(d -> new PollAnswerVoters(client, d))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets total number of voted users, if present.
     *
     * @return The total number of voted users, if present.
     */
    public Optional<Integer> getTotalVoters() {
        return Optional.ofNullable(data.totalVoters());
    }

    /**
     * Gets list of the last users that recently voted in the poll, if present.
     *
     * @return List of the last users that recently voted in the poll, if present.
     */
    public Optional<List<Id>> getRecentVoters() {
        return Optional.ofNullable(data.recentVoters())
                .map(l -> l.stream()
                        .map(i -> Id.ofUser(i, null))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets the explanation of quiz solution, if poll is quiz.
     *
     * @return The explanation of quiz solution, if poll is quiz.
     */
    public Optional<String> getSolution() {
        return Optional.ofNullable(data.solution());
    }

    /**
     * Gets list of message entities of the explanation quiz solution, if poll is quiz.
     *
     * @return The list of message entities of the explanation quiz solution, if poll is quiz.
     */
    public Optional<List<MessageEntity>> getSolutionEntities() {
        return Optional.ofNullable(data.solutionEntities())
                .map(l -> l.stream()
                        .map(d -> new MessageEntity(client, d, Objects.requireNonNull(data.solution())))
                        .collect(Collectors.toList()));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollResults that = (PollResults) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PollResults{" +
                "data=" + data +
                '}';
    }
}
