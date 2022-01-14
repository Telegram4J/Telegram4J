package telegram4j.core.object.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.TelegramObject;
import telegram4j.tl.BaseChatParticipant;
import telegram4j.tl.ChatParticipantAdmin;
import telegram4j.tl.ChatParticipantCreator;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class ChatParticipant implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.ChatParticipant data;

    public ChatParticipant(MTProtoTelegramClient client, telegram4j.tl.ChatParticipant data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Status getStatus() {
        switch (data.identifier()) {
            case BaseChatParticipant.ID: return Status.DEFAULT;
            case ChatParticipantCreator.ID: return Status.CREATOR;
            case ChatParticipantAdmin.ID: return Status.ADMIN;
            default: throw new IllegalStateException();
        }
    }

    public Id getUserId() {
        return Id.ofUser(data.userId(), null);
    }

    public Optional<Instant> getJoinTimestamp() {
        switch (data.identifier()) {
            case BaseChatParticipant.ID:
                BaseChatParticipant baseChatParticipant = (BaseChatParticipant) data;
                return Optional.of(baseChatParticipant.date()).map(Instant::ofEpochSecond);
            case ChatParticipantCreator.ID: return Optional.empty();
            case ChatParticipantAdmin.ID:
                ChatParticipantAdmin chatParticipantAdmin = (ChatParticipantAdmin) data;
                return Optional.of(chatParticipantAdmin.date()).map(Instant::ofEpochSecond);
            default: throw new IllegalStateException();
        }
    }

    public Optional<Id> getInviterId() {
        switch (data.identifier()) {
            case BaseChatParticipant.ID:
                BaseChatParticipant baseChatParticipant = (BaseChatParticipant) data;
                return Optional.of(Id.ofUser(baseChatParticipant.inviterId(), null));
            case ChatParticipantCreator.ID: return Optional.empty();
            case ChatParticipantAdmin.ID:
                ChatParticipantAdmin chatParticipantAdmin = (ChatParticipantAdmin) data;
                return Optional.of(Id.ofUser(chatParticipantAdmin.inviterId(), null));
            default: throw new IllegalStateException();
        }
    }

    public enum Status {
        DEFAULT,
        CREATOR,
        ADMIN
    }
}
