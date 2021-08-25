package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;

import java.util.Optional;

public class ChatMemberAdministrator extends ChatMember {

    public ChatMemberAdministrator(TelegramClient client, ChatMemberData data) {
        super(client, data);
    }

    public boolean isCanBeEdited() {
        return getData().canBeEdited().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isAnonymous() {
        return getData().isAnonymous().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isCanManageChat() {
        return getData().canManageChat().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isCanDeleteMessages() {
        return getData().canDeleteMessages().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isCanManageVoiceChats() {
        return getData().canManageVoiceChats().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isCanRestrictMembers() {
        return getData().canRestrictMembers().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isCanPromoteMembers() {
        return getData().canPromoteMembers().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isCanChangeInfo() {
        return getData().canChangeInfo().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean isCanInviteUsers() {
        return getData().canInviteUsers().orElseThrow(IllegalStateException::new); // must be always present
    }

    public Optional<Boolean> isCanPostMessages() {
        return getData().canPostMessages();
    }

    public Optional<Boolean> isCanEditMessages() {
        return getData().canEditMessages();
    }

    public Optional<Boolean> isCanPinMessages() {
        return getData().canPinMessages();
    }

    public Optional<String> getCustomTitle() {
        return getData().customTitle();
    }

    @Override
    public String toString() {
        return "ChatMemberAdministrator{} " + super.toString();
    }
}
