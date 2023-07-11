package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.ProfilePhoto;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.InputMessagePinned;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

sealed abstract class BaseUnavailableChat extends BaseChat
        implements UnavailableChat
        permits UnavailableChannel, UnavailableGroupChat {

    protected BaseUnavailableChat(MTProtoTelegramClient client) {
        super(client);
    }

    @Override
    public abstract Id getId();

    @Override
    public abstract Type getType();

    @Override
    public abstract String getName();

    @Override
    public Optional<ProfilePhoto> getMinPhoto() {
        return Optional.empty();
    }

    @Override
    public Optional<Photo> getPhoto() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> getMessageAutoDeleteDuration() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return Optional.empty();
    }

    @Override
    public Mono<AuxiliaryMessages> getPinnedMessage(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy)
                .getMessages(getId(), List.of(InputMessagePinned.instance()));
    }

    @Override
    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getAbout() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getFolderId() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getThemeEmoticon() {
        return Optional.empty();
    }
}
