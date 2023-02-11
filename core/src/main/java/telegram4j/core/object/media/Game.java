package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.Photo;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.object.Video;
import telegram4j.mtproto.file.Context;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.BasePhoto;

import java.util.Objects;
import java.util.Optional;

public final class Game implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.Game data;
    private final Context context;

    public Game(MTProtoTelegramClient client, telegram4j.tl.Game data, Context context) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.context = Objects.requireNonNull(context);
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
        return new Photo(client, (BasePhoto) data.photo(), context);
    }

    /**
     * Gets attached video document, if present.
     *
     * @return The attached video document, if present.
     */
    public Optional<Video> getDocument() {
        return data.document() instanceof BaseDocument d
                ? Optional.of((Video) EntityFactory.createDocument(client, d, context))
                : Optional.empty();
    }

    @Override
    public String toString() {
        return "Game{" +
                "data=" + data +
                '}';
    }
}
