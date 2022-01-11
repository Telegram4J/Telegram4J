package telegram4j.core.object.poll;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

public class PollAnswer implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PollAnswer data;

    public PollAnswer(MTProtoTelegramClient client, telegram4j.tl.PollAnswer data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public String getText() {
        return data.text();
    }

    public byte[] getOption() {
        return data.option();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollAnswer that = (PollAnswer) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PollAnswer{" +
                "data=" + data +
                '}';
    }
}
