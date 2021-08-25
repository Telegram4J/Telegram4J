package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.Id;
import telegram4j.core.object.Message;
import telegram4j.json.ChatData;
import telegram4j.json.ChatType;

import java.util.Objects;
import java.util.Optional;

class BaseChat implements Chat {

    private final TelegramClient client;
    private final ChatData data;

    BaseChat(TelegramClient client, ChatData data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public Id getId() {
        return Id.of(data.id());
    }

    @Override
    public ChatType getType() {
        return data.type();
    }

    @Override
    public ChatData getData() {
        return data;
    }

    @Override
    public Optional<ChatPhoto> getPhoto() {
        return data.photo().map(data -> new ChatPhoto(client, data));
    }

    @Override
    public Optional<Message> getPinnedMessage() {
        return data.pinnedMessage().map(data -> new Message(client, data));
    }

    @Override
    public Optional<Integer> getMessageAutoDeleteTime() {
        return data.messageAutoDeleteTime();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseChat that = (BaseChat) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "BaseChat{data=" + data + '}';
    }
}