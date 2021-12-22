package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.util.EntityParser;

import java.time.Instant;
import java.util.Optional;

@Value.Immutable
interface SendMessageSpecDef extends Spec {

    default boolean noWebpage() {
        return false;
    }

    default boolean silent() {
        return false;
    }

    default boolean background() {
        return false;
    }

    default boolean clearDraft() {
        return false;
    }

    Optional<Integer> replyToMessageId();

    String message();

    Optional<EntityParser.Mode> parseMode();

    Optional<ReplyMarkup> replyMarkup();

    // Optional<List<MessageEntity>> entities();

    Optional<Instant> scheduleTimestamp();
}
