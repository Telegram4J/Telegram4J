package telegram4j.core.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.ChatInviteLinkData;
import telegram4j.json.UserData;

import java.time.Instant;
import java.util.Optional;

public class ChatInviteLink implements TelegramObject {
    private final TelegramClient client;
    private final ChatInviteLinkData data;

    public ChatInviteLink(TelegramClient client, ChatInviteLinkData data) {
        this.client = client;
        this.data = data;
    }

    public ChatInviteLinkData getData() {
        return data;
    }

    public String getInviteLink() {
        return data.inviteLink();
    }

    public User getCreator() {
        return new User(client, data.creator());
    }

    public boolean isPrimary() {
        return data.isPrimary();
    }

    public boolean isRevoked() {
        return data.isRevoked();
    }

    public Optional<Instant> getExpireTimestamp() {
        return data.expireDate().map(Instant::ofEpochSecond);
    }

    public Optional<Integer> getMemberLimit() {
        return data.memberLimit();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatInviteLink that = (ChatInviteLink) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ChatInviteLink{" +
                "data=" + data +
                '}';
    }
}
