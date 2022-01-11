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

    // TODO list:
    // photoSizeEmpty type:string
    // photoSize type:string w:int h:int size:int
    // photoCachedSize type:string w:int h:int bytes:bytes
    // photoStrippedSize type:string bytes:bytes
    // photoSizeProgressive type:string w:int h:int sizes:Vector<int>
    // photoPathSize type:string bytes:bytes
}
