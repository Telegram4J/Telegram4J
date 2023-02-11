package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.object.User;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ExportedChatInvite implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.ChatInviteExported data;

    public ExportedChatInvite(MTProtoTelegramClient client, telegram4j.tl.ChatInviteExported data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets whether this invite link was revoked.
     *
     * @return {@code true} if this invite link was revoked.
     */
    public boolean isRevoked() {
        return data.revoked();
    }

    /**
     * Gets whether this invite link has no expiration timestamp.
     *
     * @return {@code true} if invite link has no expiration timestamp.
     */
    public boolean isPermanent() {
        return data.permanent();
    }

    /**
     * Gets whether users importing this invite link will have to
     * be approved to join the channel or group.
     *
     * @return {@code true} if link has request approving.
     */
    public boolean isRequestNeeded() {
        return data.requestNeeded();
    }

    /**
     * Gets inline link in string.
     *
     * @return The inline link in string.
     */
    public String getLink() {
        return data.link();
    }

    /**
     * Gets id of admin that created this invite link.
     *
     * @return The id of admin that created this invite link.
     */
    public Id getAdminId() {
        return Id.ofUser(data.adminId());
    }

    /**
     * Retrieve admin that created this invite.
     *
     * @return A {@link Mono} emitting on successful completion the {@link User}.
     */
    public Mono<User> getAdmin() {
        return client.getUserById(Id.ofUser(data.adminId()));
    }

    /**
     * Retrieve admin that created this invite using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return A {@link Mono} emitting on successful completion the {@link User}.
     */
    public Mono<User> getAdmin(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy)
                .getUserById(Id.ofUser(data.adminId()));
    }

    /**
     * Gets timestamp when this link created.
     *
     * @return The {@link Instant} of link creation.
     */
    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    /**
     * Gets timestamp of last link modification, if present.
     *
     * @return The {@link Instant} of last link modification, if present.
     */
    public Optional<Instant> getLastModifyTimestamp() {
        return Optional.ofNullable(data.startDate()).map(Instant::ofEpochSecond);
    }

    /**
     * Gets timestamp of link expiration, if present.
     *
     * @return The {@link Instant} of link expiration, if present.
     */
    public Optional<Instant> getExpireTimestamp() {
        return Optional.ofNullable(data.expireDate()).map(Instant::ofEpochSecond);
    }

    /**
     * Gets number of maximal link usages, if present.
     *
     * @return The number of maximal link usages, if present.
     */
    public Optional<Integer> getUsageLimit() {
        return Optional.ofNullable(data.usageLimit());
    }

    /**
     * Gets number of users joined using this link, if present.
     *
     * @return The number of users joined using this link, if present.
     */
    public Optional<Integer> getUsage() {
        return Optional.ofNullable(data.usage());
    }

    /**
     * Gets number of users that have already used this link to join, if present.
     *
     * @return The number of users that have already used this link to join, if present.
     */
    public Optional<Integer> getRequested() {
        return Optional.ofNullable(data.requested());
    }

    /**
     * Gets custom description for invite link visible only to admins, if present.
     *
     * @return The custom description for invite link visible only to admins, if present.
     */
    public Optional<String> getTitle() {
        return Optional.ofNullable(data.title());
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "ExportedChatInvite{" +
                "data=" + data +
                '}';
    }
}
