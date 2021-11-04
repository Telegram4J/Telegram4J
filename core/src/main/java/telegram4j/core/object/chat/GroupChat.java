package telegram4j.core.object.chat;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.ChatPermissions;
import telegram4j.json.ChatData;

import java.util.Optional;

public class GroupChat extends BaseChat implements GrouporizableChat {

    public GroupChat(TelegramClient client, ChatData data) {
        super(client, data);
    }

    @Override
    public Optional<String> getTitle() {
        return getData().title();
    }

    @Override
    public Optional<String> getDescription() {
        return getData().description();
    }

    @Override
    public Optional<String> getInviteLink() {
        return getData().inviteLink();
    }

    @Override
    public Optional<ChatPermissions> getPermissions() {
        return getData().permissions().map(data -> new ChatPermissions(getClient(), data));
    }

    @Override
    public String toString() {
        return "GroupChat{} " + super.toString();
    }
}
