package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.util.EntityFactory;
import telegram4j.tl.BaseWebDocument;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class WebDocument implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.WebDocument data;

    public WebDocument(MTProtoTelegramClient client, telegram4j.tl.WebDocument data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Optional<Long> getAccessHash() {
        return data instanceof telegram4j.tl.BaseWebDocument ?
                Optional.of((telegram4j.tl.BaseWebDocument) data).map(BaseWebDocument::accessHash) :
                Optional.empty();
    }

    public String getUrl() {
        return data.url();
    }

    public int getSize() {
        return data.size();
    }

    public String getMimeType() {
        return data.mimeType();
    }

    public List<DocumentAttribute> getAttributes() {
        return data.attributes().stream()
                .map(d -> EntityFactory.createDocumentAttribute(client, d))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebDocument that = (WebDocument) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "WebDocument{" +
                "data=" + data +
                '}';
    }
}
