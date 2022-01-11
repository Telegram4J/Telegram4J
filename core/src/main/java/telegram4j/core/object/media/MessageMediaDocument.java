package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Document;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseDocument;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageMediaDocument extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaDocument data;

    public MessageMediaDocument(MTProtoTelegramClient client, telegram4j.tl.MessageMediaDocument data) {
        super(client, Type.DOCUMENT);
        this.data = Objects.requireNonNull(data, "data");
    }

    public Optional<Document> getDocument() {
        return Optional.ofNullable(data.document())
                .map(d -> TlEntityUtil.unmapEmpty(d, BaseDocument.class))
                .map(d -> new Document(client, d));
    }

    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(data.ttlSeconds()).map(Duration::ofSeconds);
    }
}
