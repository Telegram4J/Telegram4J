package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.Id;
import telegram4j.core.object.Message;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.spec.SendMessageSpec;

import java.time.Duration;
import java.util.Optional;

public interface Chat extends TelegramObject {

    Id getId();

    Type getType();

    Optional<ChatPhoto> getPhoto();

    Optional<Duration> getMessageAutoDeleteDuration();

    Optional<Integer> getPinnedMessageId();

    // Interaction methods

    Mono<Message> sendMessage(SendMessageSpec spec);

    enum Type {
        GROUP, PRIVATE, SUPERGROUP, CHANNEL, UNKNOWN
    }
}
