package telegram4j.core.object.chatmember;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;

import java.time.Instant;

public final class ChatMemberRestricted extends ChatMember {

    public ChatMemberRestricted(TelegramClient client, ChatMemberData data) {
        super(client, data);
    }

    public boolean isMember() {
        return getData().isMember().orElseThrow(IllegalStateException::new);
    }

    public boolean canChangeInfo() {
        return getData().canChangeInfo().orElseThrow(IllegalStateException::new);
    }

    public boolean canInviteUsers() {
        return getData().canInviteUsers().orElseThrow(IllegalStateException::new);
    }

    public boolean canPinMessage() {
        return getData().canPinMessages().orElseThrow(IllegalStateException::new);
    }

    public boolean canSendMessage() {
        return getData().canSendMessages().orElseThrow(IllegalStateException::new);
    }

    public boolean canSendMediaMessages() {
        return getData().canSendMediaMessages().orElseThrow(IllegalStateException::new);
    }

    public boolean canSendPolls() {
        return getData().canSendPolls().orElseThrow(IllegalStateException::new);
    }

    public boolean canSendOtherMessages() {
        return getData().canSendOtherMessages().orElseThrow(IllegalStateException::new);
    }

    public boolean canAddWebPagePreview() {
        return getData().canAddWebPagePreviews().orElseThrow(IllegalStateException::new);
    }

    public Instant getUntilTimestamp() {
        return getData().untilDate().filter(i -> i > 0).map(Instant::ofEpochSecond).orElse(Instant.MIN);
    }

    @Override
    public String toString() {
        return "ChatMemberRestricted{} " + super.toString();
    }
}
