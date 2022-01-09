package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public Id getUserId() {
        return Id.ofUser(data.userId(), null);
    }

    public String getDescription() {
        return data.description();
    }

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
