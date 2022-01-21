package telegram4j.core.object.markup;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;

public abstract class ReplyMarkup implements TelegramObject {

    protected final MTProtoTelegramClient client;

    protected ReplyMarkup(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public abstract Type getType();

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "ReplyMarkup{}";
    }

    public enum Type {
        INLINE,
        FORCE,
        HIDE,
        DEFAULT;

        public static Type of(telegram4j.tl.ReplyMarkup data) {
            switch (data.identifier()) {
                case telegram4j.tl.ReplyInlineMarkup.ID: return INLINE;
                case telegram4j.tl.ReplyKeyboardForceReply.ID: return FORCE;
                case telegram4j.tl.ReplyKeyboardHide.ID: return HIDE;
                case telegram4j.tl.ReplyKeyboardMarkup.ID: return DEFAULT;
                default: throw new IllegalArgumentException("Unknown reply markup type: " + data);
            }
        }
    }
}
