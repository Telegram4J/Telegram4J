package telegram4j.core.object.media;

import telegram4j.core.object.TelegramObject;

public interface DocumentAttribute extends TelegramObject {

    Type getType();

    enum Type {
        IMAGE_SIZE,

        ANIMATED,

        STICKER,

        VIDEO,

        AUDIO,

        FILENAME,

        HAS_STICKERS
    }
}
