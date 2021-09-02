package telegram4j.core.object.chatmember;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;

import java.util.Optional;

public final class ChatMemberAdministrator extends ChatMember {

    public ChatMemberAdministrator(TelegramClient client, ChatMemberData data) {
        super(client, data);
    }

    public boolean isCanBeEdited() {
        return getData().canBeEdited().orElseThrow(IllegalStateException::new);
    }

    public boolean isAnonymous() {
        return getData().isAnonymous().orElseThrow(IllegalStateException::new);
    }

    public boolean isCanManageChat() {
        return getData().canManageChat().orElseThrow(IllegalStateException::new);
    }

    public boolean isCanDeleteMessages() {
        return getData().canDeleteMessages().orElseThrow(IllegalStateException::new);
    }

    public boolean isCanManageVoiceChats() {
        return getData().canManageVoiceChats().orElseThrow(IllegalStateException::new);
    }

    public boolean isCanRestrictMembers() {
        return getData().canRestrictMembers().orElseThrow(IllegalStateException::new);
    }

    public boolean isCanPromoteMembers() {
        return getData().canPromoteMembers().orElseThrow(IllegalStateException::new);
    }

    public boolean isCanChangeInfo() {
        return getData().canChangeInfo().orElseThrow(IllegalStateException::new);
    }

    public boolean isCanInviteUsers() {
        return getData().canInviteUsers().orElseThrow(IllegalStateException::new);
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
