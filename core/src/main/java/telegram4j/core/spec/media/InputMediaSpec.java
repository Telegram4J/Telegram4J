package telegram4j.core.spec.media;

import telegram4j.core.spec.Spec;
import telegram4j.tl.InputMedia;

public interface InputMediaSpec extends Spec {

    Type type();

    InputMedia asData();

    enum Type {
        CONTACT,
        DICE,
        DOCUMENT,
        DOCUMENT_EXTERNAL,
        GAME,
        GEO_LIVE,
        GEO_POINT,
        INVOICE,
        PHOTO,
        PHOTO_EXTERNAL,
        POLL,
        UPLOADED_DOCUMENT,
        UPLOADED_PHOTO,
        VENUE,
    }
}
