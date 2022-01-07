package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;

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
