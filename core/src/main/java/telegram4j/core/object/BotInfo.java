package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.BotCommand;
import telegram4j.tl.InputPeerEmpty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Chat bot information.
 */
public class BotInfo implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BotInfo data;

    public BotInfo(MTProtoTelegramClient client, telegram4j.tl.BotInfo data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);

    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets id of the bot, if present.
     *
     * @return The id of the bot, if present.
     */
    public Optional<Id> getBotId() {
        return Optional.ofNullable(data.userId()).map(i -> Id.ofUser(i, null));
    }

    /**
     * Gets text description of the bot.
     *
     * @return The text description of the bot.
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(data.description());
    }

    public Optional<Photo> getDescriptionPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.descriptionPhoto(), BasePhoto.class))
                .map(e -> new Photo(client, e, InputPeerEmpty.instance(), -1));
    }

    public Optional<Document> getDescriptionDocument() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.descriptionDocument(), BaseDocument.class))
                .map(d -> EntityFactory.createDocument(client, d, -1, InputPeerEmpty.instance()));
    }

    // TODO:
    // public Optional<BotMenuButton> menuButton() {
    //     return Optional.ofNullable(data.menuButton());
    // }

    /**
     * Gets list of the bot commands, if present.
     *
     * @return The {@link List} of the bot commands, if present.
     */
    public Optional<List<BotCommand>> getCommands() {
        return Optional.ofNullable(data.commands());
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
