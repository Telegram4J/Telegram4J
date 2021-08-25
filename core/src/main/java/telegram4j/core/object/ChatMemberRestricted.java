package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatMemberData;

import java.time.Instant;

public class ChatMemberRestricted extends ChatMember {

    public ChatMemberRestricted(TelegramClient client, ChatMemberData data) {
        super(client, data);
    }

    public boolean isMember() {
        return getData().isMember().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canChangeInfo() {
        return getData().canChangeInfo().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canInviteUsers() {
        return getData().canInviteUsers().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canPinMessage() {
        return getData().canPinMessages().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canSendMessage() {
        return getData().canSendMessages().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canSendMediaMessages() {
        return getData().canSendMediaMessages().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canSendPolls() {
        return getData().canSendPolls().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canSendOtherMessages() {
        return getData().canSendOtherMessages().orElseThrow(IllegalStateException::new); // must be always present
    }

    public boolean canAddWebPagePreview() {
        return getData().canAddWebPagePreviews().orElseThrow(IllegalStateException::new); // must be always present
    }

    public Instant getUntilTimestamp() {
        return getData().untilDate().filter(i -> i > 0).map(Instant::ofEpochSecond).orElse(Instant.MIN);
    }

    @Override
    public String toString() {
        return "ChatMemberRestricted{} " + super.toString();
    }
}
