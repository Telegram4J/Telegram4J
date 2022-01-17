package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import telegram4j.core.object.*;
import telegram4j.core.spec.SendMessageSpec;

import java.time.Duration;
import java.util.Optional;

public interface Chat extends PeerEntity {

    Id getId();

    Type getType();

    Optional<ChatPhoto> getMinPhoto();

    Optional<Photo> getPhoto();

    Optional<Duration> getMessageAutoDeleteDuration();

    Optional<Integer> getPinnedMessageId();

    // Interaction methods

    Mono<Message> sendMessage(SendMessageSpec spec);

    enum Type {
        PRIVATE,
        GROUP,
        SUPERGROUP,
        CHANNEL,
        UNKNOWN
    }
}
