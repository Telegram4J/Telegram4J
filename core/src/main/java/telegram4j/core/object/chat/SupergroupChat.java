package telegram4j.core.object.chat;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.ChatLocation;
import telegram4j.core.object.ChatPermissions;
import telegram4j.core.object.Id;
import telegram4j.json.ChatData;

import java.util.Optional;

public final class SupergroupChat extends BaseChat implements GrouporizableChat {

    public SupergroupChat(TelegramClient client, ChatData data) {
        super(client, data);
    }

    @Override
    public Optional<String> getTitle() {
        return getData().title();
    }

    public Optional<String> getUsername() {
        return getData().username();
    }

    @Override
    public Optional<String> getDescription() {
        return getData().description();
    }

    @Override
    public Optional<String> getInviteLink() {
        return getData().inviteLink();
    }

    public Optional<ChatPermissions> getPermissions() {
        return getData().permissions().map(data -> new ChatPermissions(getClient(), data));
    }

    public Optional<Integer> getSlowModeDelay() {
        return getData().slowModeDelay();
    }

    public Optional<String> getStickerSetName() {
        return getData().stickerSetName();
    }

    public Optional<Boolean> isCanSetStickerSet() {
        return getData().canSetStickerSet();
    }

    public Optional<Id> getLinkedChatId() {
        return getData().linkedChatId().map(Id::of);
    }

    public Optional<ChatLocation> getLocation() {
        return getData().location().map(data -> new ChatLocation(getClient(), data));
    }

    @Override
    public String toString() {
        return "SupergroupChat{} " + super.toString();
    }
}
