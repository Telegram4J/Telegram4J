package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

public class PhotoSize implements TelegramObject {

    private final MTProtoTelegramClient client;

    public PhotoSize(MTProtoTelegramClient client) {
        this.client = client;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }
}
