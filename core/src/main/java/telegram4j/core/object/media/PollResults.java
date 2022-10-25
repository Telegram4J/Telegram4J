package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.util.Id;
import telegram4j.tl.InputPeer;
import telegram4j.tl.PollAnswerVoters;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class PollResults implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PollResults data;
    private final int messageId;
    private final InputPeer peer;

    public PollResults(MTProtoTelegramClient client, telegram4j.tl.PollResults data, int messageId, InputPeer peer) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.messageId = messageId;
        this.peer = Objects.requireNonNull(peer);
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
     * @return List of poll answer results, if present otherwise empty list.
     */
    public List<PollAnswerVoters> getResults() {
        var result = data.results();
        return result != null ? result : List.of();
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
     * @return List of the last users that recently voted in the poll, if present empty list.
     */
    public List<Id> getRecentVoters() {
        return Optional.ofNullable(data.recentVoters())
                .map(l -> l.stream()
                        .map(i -> Id.ofUser(i, null))
                        .collect(Collectors.toList()))
                .orElse(List.of());
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
     * @return The list of message entities of the explanation quiz solution, if poll is quiz otherwise empty list.
     */
    public List<MessageEntity> getSolutionEntities() {
        return Optional.ofNullable(data.solutionEntities())
                .map(l -> l.stream()
                        .map(d -> new MessageEntity(client, d, Objects.requireNonNull(data.solution()), messageId, peer))
                        .collect(Collectors.toList()))
                .orElse(List.of());
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
