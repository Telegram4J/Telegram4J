package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.util.EntityParser;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

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

    Optional<Function<String, EntityParser>> parser();

    // Optional<ReplyMarkup> replyMarkup();

    // Optional<List<MessageEntity>> entities();

    Optional<Instant> scheduleTimestamp();
}
