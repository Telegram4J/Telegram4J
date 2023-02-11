package telegram4j.core.object.media;

/**
 * Type of static thumbnails for documents and photos.
 */
public sealed interface Thumbnail
        permits CachedThumbnail, PhotoThumbnail, ProgressiveThumbnail,
                StrippedThumbnail, VectorThumbnail {

    /**
     * Gets a single-char type of thumbnail representing
     * applied server-side transformations.
     *
     * @return The type of thumbnail.
     * @see <a href="https://core.telegram.org/api/files#image-thumbnail-types">Static Thumbnail Types</a>
     */
    char getType();
}
