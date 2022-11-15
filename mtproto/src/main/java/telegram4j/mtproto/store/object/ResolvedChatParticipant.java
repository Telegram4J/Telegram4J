package telegram4j.mtproto.store.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.BaseUser;
import telegram4j.tl.ChatParticipant;

import java.util.Objects;
import java.util.Optional;

public class ResolvedChatParticipant {
    private final ChatParticipant participant;
    @Nullable
    private final BaseUser user;

    public ResolvedChatParticipant(ChatParticipant participant, @Nullable BaseUser user) {
        this.participant = Objects.requireNonNull(participant);
        this.user = user;
    }

    public ChatParticipant getParticipant() {
        return participant;
    }

    public Optional<BaseUser> getUser() {
        return Optional.ofNullable(user);
    }

    @Override
    public String toString() {
        return "ResolvedChatParticipant{" +
                "participant=" + participant +
                ", user=" + user +
                '}';
    }
}
