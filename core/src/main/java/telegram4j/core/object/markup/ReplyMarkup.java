package telegram4j.core.object.markup;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

public abstract class ReplyMarkup implements TelegramObject {

    protected final MTProtoTelegramClient client;

    protected ReplyMarkup(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * Gets type of reply markup.
     *
     * @return The {@link Type} of reply markup.
     */
    public abstract Type getType();

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public enum Type {
        INLINE,
        FORCE,
        HIDE,
        KEYBOARD;

        public static Type of(telegram4j.tl.ReplyMarkup data) {
            switch (data.identifier()) {
                case telegram4j.tl.ReplyInlineMarkup.ID: return INLINE;
                case telegram4j.tl.ReplyKeyboardForceReply.ID: return FORCE;
                case telegram4j.tl.ReplyKeyboardHide.ID: return HIDE;
                case telegram4j.tl.ReplyKeyboardMarkup.ID: return KEYBOARD;
                default: throw new IllegalArgumentException("Unknown reply markup type: " + data);
            }
        }
    }
}
