package telegram4j.core.object.media;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.Photo;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.object.Video;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.InputPeer;

import java.util.Objects;
import java.util.Optional;

public final class Game implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.Game data;
    private final int messageId;
    private final InputPeer peer;

    public Game(MTProtoTelegramClient client, telegram4j.tl.Game data, int messageId, InputPeer peer) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);

        this.messageId = messageId;
        this.peer = Objects.requireNonNull(peer);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets id of the game.
     *
     * @return The id of the game.
     */
    public long getId() {
        return data.id();
    }

    /**
     * Gets access hash of the game.
     *
     * @return The access hash of the game.
     */
    public long getAccessHash() {
        return data.accessHash();
    }

    /**
     * Gets short name of the game.
     *
     * @return The short name of the game.
     */
    public String getShortName() {
        return data.shortName();
    }

    /**
     * Gets title of the game.
     *
     * @return The title of the game.
     */
    public String getTitle() {
        return data.title();
    }

    /**
     * Gets description of the game.
     *
     * @return The description of the game.
     */
    public String getDescription() {
        return data.description();
    }

    /**
     * Gets preview for the game.
     *
     * @return The preview for the game.
     */
    public Photo getPhoto() {
        BasePhoto p = TlEntityUtil.unmapEmpty(data.photo(), BasePhoto.class);
        Objects.requireNonNull(p);
        return new Photo(client, p, peer, messageId);
    }

    /**
     * Gets attached video document, if present.
     *
     * @return The attached video document, if present.
     */
    public Optional<Video> getDocument() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.document(), BaseDocument.class))
                .map(d -> (Video) EntityFactory.createDocument(client, d, messageId, peer));
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
