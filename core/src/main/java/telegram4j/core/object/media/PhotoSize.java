package telegram4j.core.object.media;

import telegram4j.core.object.TelegramObject;

/** Thumbnail type of document/photo. */
public interface PhotoSize extends TelegramObject {

    /**
     * Gets a single-char <a href="https://core.telegram.org/api/files#image-thumbnail-types">type</a> of thumbnail
     *
     * @return The type of thumbnail.
     */
    char getType();
}
