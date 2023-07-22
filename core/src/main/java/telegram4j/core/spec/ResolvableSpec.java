package telegram4j.core.spec;

import telegram4j.core.MTProtoTelegramClient;

public interface ResolvableSpec<T> {

    T resolve(MTProtoTelegramClient client);
}
