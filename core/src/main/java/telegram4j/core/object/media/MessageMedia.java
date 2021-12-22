package telegram4j.core.object.media;

import telegram4j.core.object.TelegramObject;

public interface MessageMedia extends TelegramObject {

    Type getType();

    enum Type {

        /** Attached photo. */
        PHOTO,

        /** Attached map. */
        GEO,

        /** Attached contact. */
        CONTACT,

        /** Current version of the client does not support this media type. */
        UNSUPPORTED,

        /** Document (video, audio, voice, sticker, any media type except photo). */
        DOCUMENT,

        /** Preview of webpage. */
        WEB_PAGE,

        /** Message venue. */
        VENUE,

        /** Telegram game. */
        GAME,

        /** Payment invoice. */
        INVOICE,

        /** Indicates a live geolocation. */
        GEO_LIVE,

        /** Message poll. */
        POLL,

        /** Message dice. */
        DICE
    }
}
