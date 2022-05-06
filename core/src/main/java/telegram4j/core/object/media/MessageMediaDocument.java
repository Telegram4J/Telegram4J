package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Document;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.InputPeer;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageMediaDocument extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaDocument data;
    private final int messageId;
    private final InputPeer peer;

    public MessageMediaDocument(MTProtoTelegramClient client, telegram4j.tl.MessageMediaDocument data,
                                int messageId, InputPeer peer) {
        super(client, Type.DOCUMENT);
        this.data = Objects.requireNonNull(data, "data");
        this.messageId = messageId;
        this.peer = Objects.requireNonNull(peer, "peer");
    }

    /**
     * Gets document of the message, if it hasn't expired.
     *
     * @return The {@link Document} of the message, if it hasn't expired.
     */
    public Optional<Document> getDocument() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.document(), BaseDocument.class))
                .map(d -> EntityFactory.createDocument(client, d, messageId, peer));
    }

    /**
     * Gets {@link Duration} of the document self-destruction, if present.
     *
     * @return The {@link Duration} of the document self-destruction, if present.
     */
    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(data.ttlSeconds()).map(Duration::ofSeconds);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaDocument that = (MessageMediaDocument) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMediaDocument{" +
                "data=" + data +
                '}';
    }
}
