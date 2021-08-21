package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.ChatData;
import telegram4j.json.ChatType;

import java.util.Objects;
import java.util.Optional;

public class Chat implements TelegramObject {

    private final TelegramClient client;
    private final ChatData data;

    public Chat(TelegramClient client, ChatData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public Id getId() {
        return Id.of(data.id());
    }

    public ChatType getType() {
        return data.type();
    }

    public Optional<Boolean> isAllMembersAreAdministrators() {
        return data.allMembersAreAdministrators();
    }

    public Optional<String> getTitle() {
        return data.title();
    }

    public Optional<String> getUsername() {
        return data.username();
    }

    public Optional<String> getFirstName() {
        return data.firstName();
    }

    public Optional<String> getLastName() {
        return data.lastName();
    }

    public Optional<ChatPhoto> getPhoto() {
        return data.photo().map(data -> new ChatPhoto(client, data));
    }

    public Optional<String> getBio() {
        return data.bio();
    }

    public Optional<String> getDescription() {
        return data.description();
    }

    public Optional<String> getInviteLink() {
        return data.inviteLink();
    }

    public Optional<Message> getPinnedMessage() {
        return data.pinnedMessage().map(data -> new Message(client, data));
    }

    public Optional<ChatPermissions> getPermissions() {
        return data.permissions().map(data -> new ChatPermissions(client, data));
    }

    public Optional<Integer> getSlowModeDelay() {
        return data.slowModeDelay();
    }

    public Optional<Integer> getMessageAutoDeleteTime() {
        return data.messageAutoDeleteTime();
    }

    public Optional<String> getStickerSetName() {
        return data.stickerSetName();
    }

    public boolean isCanSetStickerSet() {
        return data.canSetStickerSet().orElse(false);
    }

    public Optional<Id> getLinkedChatId() {
        return data.linkedChatId().map(Id::of);
    }

    public Optional<ChatLocation> getLocation() {
        return data.location().map(data -> new ChatLocation(client, data));
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat that = (Chat) o;
        return getId().equals(that.getId());
    }

    @Override
    public String toString() {
        return "Chat{data=" + data + '}';
    }
}
