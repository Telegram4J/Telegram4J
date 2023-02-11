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

/**
 * Chat bot information.
 */
public final class BotInfo implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.BotInfo data;
    private final Id peer;
    @Nullable
    private final User resolvedBot;

    public BotInfo(MTProtoTelegramClient client, telegram4j.tl.BotInfo data,
                   Id peer, @Nullable User resolvedBot) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.peer = Objects.requireNonNull(peer);
        this.resolvedBot = resolvedBot;
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
                .orElseThrow(() -> new IllegalStateException("BotInfo.getBotId() is absent")); // TODO: need to verify
    }

    /**
     * Gets bot described by this info, if from full channel or group chat.
     *
     * @return The bot described by this info, if from full channel or group chat.
     */
    public Optional<User> getResolvedBot() {
        return Optional.ofNullable(resolvedBot);
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

    /**
     * Gets bot description photo or gif document, if present.
     *
     * @return The bot description {@link Photo} or {@link Video} document, if present.
     */
    public Optional<Document> getDescriptionDocument() {
        if (!(data.descriptionDocument() instanceof BaseDocument b)) {
            return data.descriptionPhoto() instanceof BasePhoto p
                    ? Optional.of(new Photo(client, p, Context.createBotInfoContext(peer.asPeer(), getBotId().asLong())))
                    : Optional.empty();
        }

        return Optional.of(EntityFactory.createDocument(client, b,
                Context.createBotInfoContext(peer.asPeer(), getBotId().asLong())));
    }

    // TODO bot menu support?

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
    public String toString() {
        return "BotInfo{" +
                "data=" + data +
                '}';
    }
}
