package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;

/** Client-associated object of the Telegram API. */
public interface TelegramObject {

    /** @return The {@link MTProtoTelegramClient} associated to this object. */
    MTProtoTelegramClient getClient();
}
