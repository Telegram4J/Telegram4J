package telegram4j.core.object.poll;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

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

    public boolean isChosen() {
        return data.chosen();
    }

    public boolean isCorrect() {
        return data.correct();
    }

    public byte[] getOption() {
        return data.option();
    }

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
