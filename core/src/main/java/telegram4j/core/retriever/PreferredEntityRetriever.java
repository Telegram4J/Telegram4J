package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.tl.InputMessage;

/**
 * Additional wrapping for {@code EntityRetriever} which have settings to control
 * behavior of {@link #getUserById(Id)} and {@link #getChatById(Id)} methods.
 */
public class PreferredEntityRetriever implements EntityRetriever {
    private final EntityRetriever delegate;
    private final Setting chatPreference;
    private final Setting userPreference;

    PreferredEntityRetriever(EntityRetriever delegate, Setting chatPreference, Setting userPreference) {
        this.delegate = delegate;
        this.chatPreference = chatPreference;
        this.userPreference = userPreference;
    }

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        return delegate.resolvePeer(peerId);
    }

    @Override
    public Mono<User> getUserById(Id userId) {
        switch (userPreference) {
            case MIN: return delegate.getUserMinById(userId);
            case FULL: return delegate.getUserFullById(userId);
            case NONE: return delegate.getUserById(userId);
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        return delegate.getUserMinById(userId);
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        return delegate.getUserFullById(userId);
    }

    @Override
    public Mono<Chat> getChatById(Id chatId) {
        switch (chatPreference) {
            case MIN: return delegate.getChatMinById(chatId);
            case FULL: return delegate.getChatFullById(chatId);
            case NONE: return delegate.getChatById(chatId);
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return delegate.getChatMinById(chatId);
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return delegate.getChatFullById(chatId);
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(@Nullable Id chatId, Iterable<? extends InputMessage> messageIds) {
        return delegate.getMessagesById(chatId, messageIds);
    }

    /** Types of volumes of returned information. */
    public enum Setting {

        /** Option to use delegate's behavior. */
        NONE,

        /** Option for returning min objects. */
        MIN,

        /** Option for returning full objects. */
        FULL
    }
}
