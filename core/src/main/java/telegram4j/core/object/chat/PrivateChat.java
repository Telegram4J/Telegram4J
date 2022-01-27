package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.User;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Represents a direct message chat. */
public final class PrivateChat extends BaseChat {

    private final User user;
    private final User selfUser;

    public PrivateChat(MTProtoTelegramClient client, User user, @Nullable User selfUser) {
        super(client, user.getId(), Type.PRIVATE);
        this.user = Objects.requireNonNull(user, "user");
        this.selfUser = selfUser;
    }

    /**
     * Gets the interlocutor user.
     *
     * @return The {@link User} interlocutor.
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the self user, if present.
     *
     * @return The self {@link User} of DM, if present.
     */
    public Optional<User> getSelfUser() {
        return Optional.ofNullable(selfUser);
    }

    @Override
    public Optional<ChatPhoto> getMinPhoto() {
        return user.getMinPhoto();
    }

    @Override
    public Optional<Photo> getPhoto() {
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
    public Optional<PeerNotifySettings> getNotifySettings() {
        return user.getNotifySettings();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrivateChat that = (PrivateChat) o;
        return user.equals(that.user) && Objects.equals(selfUser, that.selfUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, selfUser);
    }

    @Override
    public String toString() {
        return "PrivateChat{" +
                "user=" + user +
                ", selfUser=" + selfUser +
                '}';
    }
}
