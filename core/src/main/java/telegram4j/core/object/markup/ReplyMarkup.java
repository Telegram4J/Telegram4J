package telegram4j.core.object.markup;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

public abstract class ReplyMarkup implements TelegramObject {

    protected final MTProtoTelegramClient client;

    protected ReplyMarkup(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
    }

    /**
     * Gets type of reply markup.
     *
     * @return The {@link Type} of reply markup.
     */
    public abstract Type getType();

    @Override
    public final MTProtoTelegramClient getClient() {
        return client;
    }

    public enum Type {
        INLINE,
        FORCE,
        HIDE,
        KEYBOARD
    }
}
