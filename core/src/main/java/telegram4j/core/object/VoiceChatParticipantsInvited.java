package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.VoiceChatParticipantsInvitedData;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class VoiceChatParticipantsInvited implements TelegramObject {

    private final TelegramClient client;
    private final VoiceChatParticipantsInvitedData data;

    public VoiceChatParticipantsInvited(TelegramClient client, VoiceChatParticipantsInvitedData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public VoiceChatParticipantsInvitedData getData() {
        return data;
    }

    public Optional<List<User>> getUsers() {
        return data.users().map(list -> list.stream()
                .map(data -> new User(client, data))
                .collect(Collectors.toList()));
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoiceChatParticipantsInvited that = (VoiceChatParticipantsInvited) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "VoiceChatParticipantsInvited{data=" + data + '}';
    }
}
