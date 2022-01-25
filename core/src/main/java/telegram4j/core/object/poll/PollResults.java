package telegram4j.core.object.poll;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.TelegramObject;

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

    public boolean isMin() {
        return data.min();
    }

    public Optional<List<PollAnswerVoters>> getResults() {
        return Optional.ofNullable(data.results())
                .map(l -> l.stream()
                        .map(d -> new PollAnswerVoters(client, d))
                        .collect(Collectors.toList()));
    }

    public Optional<Integer> getTotalVoters() {
        return Optional.ofNullable(data.totalVoters());
    }

    public Optional<List<Long>> getRecentVoters() {
        return Optional.ofNullable(data.recentVoters());
    }

    public Optional<String> getSolution() {
        return Optional.ofNullable(data.solution());
    }

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
