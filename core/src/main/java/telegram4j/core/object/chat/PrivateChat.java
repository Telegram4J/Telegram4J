package telegram4j.core.object.chat;

import telegram4j.core.TelegramClient;
import telegram4j.json.ChatData;

import java.util.Optional;

public final class PrivateChat extends BaseChat {

    public PrivateChat(TelegramClient client, ChatData data) {
        super(client, data);
    }

    public Optional<String> getUsername() {
        return getData().username();
    }

    public Optional<String> getFirstName() {
        return getData().firstName();
    }

    public Optional<String> getLastName() {
        return getData().lastName();
    }

    public Optional<String> getBio() {
        return getData().bio();
    }

    @Override
    public String toString() {
        return "PrivateChat{} " + super.toString();
    }
}
