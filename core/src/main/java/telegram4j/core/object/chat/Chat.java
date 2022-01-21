package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.object.*;
import telegram4j.core.spec.ForwardMessagesSpec;
import telegram4j.core.spec.SendMessageSpec;

import java.time.Duration;
import java.util.Optional;

/** The Telegram <a href="https://core.telegram.org/api/channel">chat</a> representation. */
public interface Chat extends PeerEntity {

    Id getId();

    /**
     * Gets the type of chat.
     *
     * @return The {@link Type type} of chat.
     */
    Type getType();

    /**
     * Gets the low quality chat photo, if present.
     *
     * @return The {@link ChatPhoto photo} of chat, if present.
     */
    Optional<ChatPhoto> getMinPhoto();

    /**
     * Gets the normal chat photo, if present
     * and if detailed information about chat is available.
     *
     * @return The {@link Photo photo} of chat, if present and available.
     */
    Optional<Photo> getPhoto();

    Optional<Duration> getMessageAutoDeleteDuration();

    Optional<Integer> getPinnedMessageId();

    // Interaction methods

    Mono<Message> sendMessage(SendMessageSpec spec);

    Flux<Message> forwardMessages(ForwardMessagesSpec spec, PeerId toPeer);

    /** All types of telegram chat. */
    enum Type {

        /** Represents a {@link PrivateChat}. */
        PRIVATE,

        /** Represents a {@link GroupChat}. */
        GROUP,

        /** Represents a {@link SupergroupChat}. */
        SUPERGROUP,

        /** Represents a {@link BroadcastChannel}. */
        CHANNEL;
    }
}
