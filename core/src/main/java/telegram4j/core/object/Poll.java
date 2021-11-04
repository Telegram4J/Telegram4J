package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.PollData;
import telegram4j.json.PollType;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Poll implements TelegramObject {

    private final TelegramClient client;
    private final PollData data;

    public Poll(TelegramClient client, PollData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public PollData getData() {
        return data;
    }

    public String getId() {
        return data.id();
    }

    public String getQuestion() {
        return data.question();
    }

    public List<PollOption> getOptions() {
        return data.options().stream()
                .map(data -> new PollOption(client, data))
                .collect(Collectors.toList());
    }

    public int getTotalVoterCount() {
        return data.totalVoterCount();
    }

    public boolean isClosed() {
        return data.isClosed();
    }

    public boolean isAnonymous() {
        return data.isAnonymous();
    }

    public PollType getType() {
        return data.type();
    }

    public boolean isAllowsMultipleAnswers() {
        return data.allowsMultipleAnswers();
    }

    public Optional<Integer> getCorrectOptionId() {
        return data.correctOptionId();
    }

    public Optional<String> getExplanation() {
        return data.explanation();
    }

    public Optional<List<MessageEntity>> getExplanationEntities() {
        return data.explanationEntities().map(list -> list.stream()
                .map(data -> new MessageEntity(client, data,
                        getExplanation().orElseThrow(IllegalStateException::new)))
                .collect(Collectors.toList()));
    }

    public Optional<Integer> getOpenPeriod() {
        return data.openPeriod();
    }

    public Optional<Instant> getCloseTimestamp() {
        return data.closeDate().map(Instant::ofEpochSecond);
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Poll that = (Poll) o;
        return data.equals(that.data);
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
