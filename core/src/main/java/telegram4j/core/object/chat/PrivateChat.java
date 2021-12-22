package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.User;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class PrivateChat extends BaseChat {

    private final User user;

    public PrivateChat(MTProtoTelegramClient client, User user) {
        super(client, user.getId(), Type.PRIVATE);
        this.user = Objects.requireNonNull(user, "user");
    }

    public User getUser() {
        return user;
    }

    @Override
    public Optional<ChatPhoto> getPhoto() {
        return user.getPhoto();
    }

    @Override
    public Optional<Duration> getMessageAutoDeleteDuration() {
        return user.getMessageAutoDeleteDuration();
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return user.getPinnedMessageId();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PrivateChat that = (PrivateChat) o;
        return user.equals(that.user);
    }

    @Override
    public int hashCode() {
        return user.hashCode();
    }

    @Override
    public String toString() {
        return "PrivateChat{" +
                "user=" + user +
                "} " + super.toString();
    }
}
