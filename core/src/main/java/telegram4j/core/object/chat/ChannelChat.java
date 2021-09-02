package telegram4j.core.object.chat;

import telegram4j.core.TelegramClient;
import telegram4j.json.api.Id;
import telegram4j.json.ChatData;

import java.util.Optional;

public final class ChannelChat extends BaseChat implements LinkedChat {

    public ChannelChat(TelegramClient client, ChatData data) {
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
    public Optional<Id> getLinkedChatId() {
        return getData().linkedChatId().map(Id::of);
    }

    @Override
    public String toString() {
        return "ChannelChat{} " + super.toString();
    }
}
