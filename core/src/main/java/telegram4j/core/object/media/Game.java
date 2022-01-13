package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Document;
import telegram4j.core.object.Photo;
import telegram4j.core.object.TelegramObject;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.BasePhoto;

import java.util.Objects;
import java.util.Optional;

public class Game implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.Game data;

    public Game(MTProtoTelegramClient client, telegram4j.tl.Game data) {
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

    public long getAccessHash() {
        return data.accessHash();
    }

    public String getShortName() {
        return data.shortName();
    }

    public String getTitle() {
        return data.title();
    }

    public String getDescription() {
        return data.description();
    }

    public Optional<Photo> getPhoto() {
        return Optional.of(data.photo())
                .map(p -> TlEntityUtil.unmapEmpty(p, BasePhoto.class))
                .map(p -> new Photo(client, p));
    }

    public Optional<Document> getDocument() {
        return Optional.ofNullable(data.document())
                .map(d -> TlEntityUtil.unmapEmpty(d, BaseDocument.class))
                .map(d -> new Document(client, d));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return data.equals(game.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Game{" +
                "data=" + data +
                '}';
    }
}