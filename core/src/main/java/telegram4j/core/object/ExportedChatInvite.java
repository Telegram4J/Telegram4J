package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class ExportedChatInvite implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.ChatInviteExported data;
    private final User admin;

    public ExportedChatInvite(MTProtoTelegramClient client, telegram4j.tl.ChatInviteExported data, User admin) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.admin = admin;
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
     * Gets inline link in string.
     *
     * @return The inline link in string.
     */
    public String getLink() {
        return data.link();
    }

    /**
     * Gets admin that created this invite.
     *
     * @return The {@link User} that created this invite.
     */
    public User getAdmin() {
        return admin;
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
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportedChatInvite that = (ExportedChatInvite) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ExportedChatInvite{" +
                "data=" + data +
                ", admin=" + admin +
                '}';
    }
}
