package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.User;
import telegram4j.core.util.Id;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Represents a direct message chat. */
public final class PrivateChat extends BaseChat {

    private final User user;
    private final User selfUser;

    public PrivateChat(MTProtoTelegramClient client, User user, @Nullable User selfUser) {
        super(client);
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
    public Id getId() {
        return user.getId();
    }

    @Override
    public Type getType() {
        return Type.PRIVATE;
    }

    @Override
    public String getName() {
        return user.getFullName();
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
    public Optional<String> getAbout() {
        return user.getAbout();
    }

    @Override
    public Optional<Integer> getFolderId() {
        return user.getFolderId();
    }

    @Override
    public Optional<String> getThemeEmoticon() {
        return user.getThemeEmoticon();
    }

    @Override
    public String toString() {
        return "PrivateChat{" +
                "user=" + user +
                ", selfUser=" + selfUser +
                '}';
    }
}
