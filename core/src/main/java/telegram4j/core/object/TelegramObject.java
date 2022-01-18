package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;

/** Client-associated object of the Telegram API. */
public interface TelegramObject {

    /**
     * Gets {@link MTProtoTelegramClient client} associated to this object.
     *
     * @return The {@link MTProtoTelegramClient} associated to this object.
     */
    MTProtoTelegramClient getClient();
}
