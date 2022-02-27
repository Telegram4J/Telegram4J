package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class MessageForwardHeader implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageFwdHeader data;

    public MessageForwardHeader(MTProtoTelegramClient client, telegram4j.tl.MessageFwdHeader data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public boolean isImported() {
        return data.imported();
    }

    public Optional<Id> getFromId() {
        return Optional.ofNullable(data.fromId()).map(Id::of);
    }

    public Optional<String> getFromName() {
        return Optional.ofNullable(data.fromName());
    }

    public Instant getOriginalTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    public Optional<Integer> getChannelPostId() {
        return Optional.ofNullable(data.channelPost());
    }

    public Optional<String> getPostAuthor() {
        return Optional.ofNullable(data.postAuthor());
    }

    public Optional<Id> getSavedFromPeerId() {
        return Optional.ofNullable(data.savedFromPeer()).map(Id::of);
    }

    public Optional<Integer> getSavedFromMessageId() {
        return Optional.ofNullable(data.savedFromMsgId());
    }

    public Optional<String> getPsaType() {
        return Optional.ofNullable(data.psaType());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageForwardHeader that = (MessageForwardHeader) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageForwardHeader{" +
                "data=" + data +
                '}';
    }
}
