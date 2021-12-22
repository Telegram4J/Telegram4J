package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;
import java.util.Optional;

public class MessageReplyHeader implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageReplyHeader data;

    public MessageReplyHeader(MTProtoTelegramClient client, telegram4j.tl.MessageReplyHeader data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public int getReplyToMsgId() {
        return data.replyToMsgId();
    }

    public Optional<Id> getReplyToPeerId() {
        return Optional.ofNullable(data.replyToPeerId()).map(Id::of);
    }

    public Optional<Integer> getReplyToTopId() {
        return Optional.ofNullable(data.replyToTopId());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageReplyHeader that = (MessageReplyHeader) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageReplyHeader{" +
                "data=" + data +
                '}';
    }
}
