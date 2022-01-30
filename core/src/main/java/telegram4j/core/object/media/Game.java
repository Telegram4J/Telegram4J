package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Document;
import telegram4j.core.object.Photo;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.InputPeer;

import java.util.Objects;
import java.util.Optional;

public class Game implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.Game data;
    private final int messageId;
    private final InputPeer peer;

    public Game(MTProtoTelegramClient client, telegram4j.tl.Game data, int messageId, InputPeer peer) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");

        this.messageId = messageId;
        this.peer = Objects.requireNonNull(peer, "peer");
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
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.photo(), BasePhoto.class))
                .map(p -> new Photo(client, p, messageId));
    }

    public Optional<Document> getDocument() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.document(), BaseDocument.class))
                .map(d -> EntityFactory.createDocument(client, d, messageId, peer));
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
