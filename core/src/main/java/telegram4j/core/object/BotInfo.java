package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Chat bot information.
 */
public class BotInfo implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BotInfo data;

    public BotInfo(MTProtoTelegramClient client, telegram4j.tl.BotInfo data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets id of the bot.
     *
     * @return The id of the bot.
     */
    public Id getBotId() {
        return Id.ofUser(data.userId(), null);
    }

    /**
     * Gets text description of the bot.
     *
     * @return The text description of the bot.
     */
    public String getDescription() {
        return data.description();
    }

    /**
     * Gets list of the bot commands.
     *
     * @return The {@link List} of the bot commands.
     */
    public List<BotCommand> getCommands() {
        return data.commands().stream()
                .map(d -> new BotCommand(client, d))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotInfo botInfo = (BotInfo) o;
        return data.equals(botInfo.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "BotInfo{" +
                "data=" + data +
                '}';
    }
}
