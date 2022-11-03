package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.mtproto.file.Context;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.BotCommand;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static telegram4j.mtproto.util.TlEntityUtil.unmapEmpty;

/**
 * Chat bot information.
 */
public class BotInfo implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BotInfo data;
    private final Id peer;

    public BotInfo(MTProtoTelegramClient client, telegram4j.tl.BotInfo data, Id peer) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.peer = Objects.requireNonNull(peer);
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
    public Id getBotId() {
        return Optional.ofNullable(data.userId())
                .map(Id::ofUser)
                .or(() -> peer.getType() == Id.Type.USER
                        ? Optional.of(peer)
                        : Optional.empty())
                .orElseThrow(() -> new IllegalStateException("Peer: " + peer)); // need to verify
    }

    /**
     * Requests to retrieve bot described by this info.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User user}.
     */
    public Mono<User> getBot() {
        return getBot(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve bot described by this info using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User user}.
     */
    public Mono<User> getBot(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy).getUserById(getBotId());
    }

    /**
     * Gets text description of the bot.
     *
     * @return The text description of the bot.
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(data.description());
    }

    public Optional<Document> getDescriptionDocument() {
        return Optional.ofNullable(unmapEmpty(data.descriptionDocument(), BaseDocument.class))
                .map(d -> EntityFactory.createDocument(client, d,
                        Context.createBotInfoContext(peer.asPeer(), getBotId().asLong())))
                .or(() -> Optional.ofNullable(unmapEmpty(data.descriptionPhoto(), BasePhoto.class))
                        .map(e -> new Photo(client, e, Context.createBotInfoContext(peer.asPeer(),
                                getBotId().asLong()))));
    }

    // TODO:
    // public Optional<BotMenuButton> menuButton() {
    //     return Optional.ofNullable(data.menuButton());
    // }

    /**
     * Gets immutable list of the bot commands, if present.
     *
     * @return The immutable list of the bot commands, if present otherwise empty list.
     */
    public List<BotCommand> getCommands() {
        var commands = data.commands();
        return commands != null ? commands : List.of();
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
