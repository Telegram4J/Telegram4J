package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.ChatPermissionsData;

import java.util.Objects;
import java.util.Optional;

public class ChatPermissions implements TelegramObject {

    private final TelegramClient client;
    private final ChatPermissionsData data;

    public ChatPermissions(TelegramClient client, ChatPermissionsData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public ChatPermissionsData getData() {
        return data;
    }

    public Optional<Boolean> isCanSendMessages() {
        return data.canSendMessages();
    }

    public Optional<Boolean> isCanSendMediaMessages() {
        return data.canSendMediaMessages();
    }

    public Optional<Boolean> isCanSendPolls() {
        return data.canSendPolls();
    }

    public Optional<Boolean> isCanSendOtherMessages() {
        return data.canSendOtherMessages();
    }

    public Optional<Boolean> isCanAddWebPagePreviews() {
        return data.canAddWebPagePreviews();
    }

    public Optional<Boolean> isCanChangeInfo() {
        return data.canChangeInfo();
    }

    public Optional<Boolean> isCanInviteUsers() {
        return data.canInviteUsers();
    }

    public Optional<Boolean> isCanPinMessages() {
        return data.canPinMessages();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatPermissions that = (ChatPermissions) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ChatPermissions{data=" + data + '}';
    }
}
