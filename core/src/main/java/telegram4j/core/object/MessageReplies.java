package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MessageReplies implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageReplies data;

    public MessageReplies(MTProtoTelegramClient client, telegram4j.tl.MessageReplies data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public boolean isComments() {
        return data.comments();
    }

    public int getReplies() {
        return data.replies();
    }

    public int getRepliesPts() {
        return data.repliesPts();
    }

    public Optional<List<Id>> getRecentRepliers() {
        return Optional.ofNullable(data.recentRepliers()).map(list -> list.stream()
                .map(Id::of)
                .collect(Collectors.toList()));
    }

    public Optional<Id> getChannelId() {
        return Optional.ofNullable(data.channelId()).map(c -> Id.ofChannel(c, null));
    }

    public Optional<Integer> getMaxId() {
        return Optional.ofNullable(data.maxId());
    }

    public Optional<Integer> getReadMaxId() {
        return Optional.ofNullable(data.readMaxId());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageReplies that = (MessageReplies) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageReplies{" +
                "data=" + data +
                '}';
    }
}
