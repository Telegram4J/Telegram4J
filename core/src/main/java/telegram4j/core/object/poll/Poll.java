package telegram4j.core.object.poll;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Poll implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.Poll data;

    public Poll(MTProtoTelegramClient client, telegram4j.tl.Poll data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public long getId() {
        return data.id();
    }

    public boolean isClosed() {
        return data.closed();
    }

    public boolean isPublicVoters() {
        return data.publicVoters();
    }

    public boolean isMultipleChoice() {
        return data.multipleChoice();
    }

    public boolean isQuiz() {
        return data.quiz();
    }

    public String getQuestion() {
        return data.question();
    }

    public List<PollAnswer> getAnswers() {
        return data.answers().stream()
                .map(d -> new PollAnswer(client, d))
                .collect(Collectors.toList());
    }

    public Optional<Duration> getClosePeriod() {
        return Optional.ofNullable(data.closePeriod()).map(Duration::ofSeconds);
    }

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
        return "Poll{" +
                "data=" + data +
                '}';
    }
}
