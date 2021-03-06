package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

/** Bot command object. */
public class BotCommand implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BotCommand data;

    public BotCommand(MTProtoTelegramClient client, telegram4j.tl.BotCommand data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets name of command in format <b>/command name</b>.
     *
     * @return The name of command.
     */
    public String getCommand() {
        return data.command();
    }

    /**
     * Gets text description of command.
     *
     * @return The text description of command.
     */
    public String getDescription() {
        return data.description();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotCommand that = (BotCommand) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "BotCommand{" +
                "data=" + data +
                '}';
    }
}
