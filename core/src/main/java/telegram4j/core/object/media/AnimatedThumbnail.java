package telegram4j.core.object.media;

/**
 * Type of animated thumbnail for profile photos.
 */
public sealed interface AnimatedThumbnail permits VideoThumbnail, StickerThumbnail {}
