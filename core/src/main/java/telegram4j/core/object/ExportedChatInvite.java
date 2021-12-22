package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Optional;

public class ExportedChatInvite implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.ExportedChatInvite data;

    public ExportedChatInvite(MTProtoTelegramClient client, telegram4j.tl.ExportedChatInvite data) {
        this.client = client;
        this.data = data;
    }

    public boolean isRevoked() {
        return data.revoked();
    }

    public boolean isPermanent() {
        return data.permanent();
    }

    public String getLink() {
        return data.link();
    }

    public Id getAdminId() {
        return Id.of(data.adminId());
    }

    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    public Optional<Instant> getLastModifyTimestamp() {
        return Optional.ofNullable(data.startDate()).map(Instant::ofEpochSecond);
    }

    public Optional<Instant> getExpireTimestamp() {
        return Optional.ofNullable(data.expireDate()).map(Instant::ofEpochSecond);
    }

    public Optional<Integer> getUsageLimit() {
        return Optional.ofNullable(data.usageLimit());
    }

    public Optional<Integer> getUsage() {
        return Optional.ofNullable(data.usage());
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
                '}';
    }
}
