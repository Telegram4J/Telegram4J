package telegram4j.core.object.media;

/** Thumbnail type of document/photo. */
public interface PhotoSize {

    /**
     * Gets a single-char <a href="https://core.telegram.org/api/files#image-thumbnail-types">type</a> of thumbnail
     *
     * @return The type of thumbnail.
     */
    char getType();
}
