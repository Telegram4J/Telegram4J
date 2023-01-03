package telegram4j.core.object.media;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.object.User;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.PollAnswerVoters;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PollResults implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PollResults data;

    public PollResults(MTProtoTelegramClient client, telegram4j.tl.PollResults data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
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
     * Gets set of the last users that recently voted in the poll, if present.
     *
     * @return The set of the last users that recently voted in the poll, if present empty list.
     */
    public Set<Id> getRecentVotersIds() {
        return Optional.ofNullable(data.recentVoters())
                .map(l -> l.stream()
                        .map(Id::ofUser)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /**
     * Requests to retrieve the recent poll voters.
     *
     * @return A {@link Flux} which emits the {@link User voters}.
     */
    public Flux<User> getRecentVoters() {
        return getRecentVoters(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve the recent poll voters using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return A {@link Flux} which emits the {@link User voters}.
     */
    public Flux<User> getRecentVoters(EntityRetrievalStrategy strategy) {
        var retriever = client.withRetrievalStrategy(strategy);
        return Mono.justOrEmpty(data.recentVoters())
                .flatMapIterable(Function.identity())
                .flatMap(id -> retriever.getUserById(Id.ofUser(id)));
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
                        .map(d -> new MessageEntity(client, d, Objects.requireNonNull(data.solution())))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    public String toString() {
        return "PollResults{" +
                "data=" + data +
                '}';
    }
}
