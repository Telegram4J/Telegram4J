package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.util.EntityParser;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

@Value.Immutable
interface SendMessageSpecDef extends Spec {

    @Value.Default
    default boolean noWebpage() {
        return false;
    }

    @Value.Default
    default boolean silent() {
        return false;
    }

    @Value.Default
    default boolean background() {
        return false;
    }

    @Value.Default
    default boolean clearDraft() {
        return false;
    }

    Optional<Integer> replyToMessageId();

    String message();

    Optional<Function<String, EntityParser>> parser();

    Optional<MessageFields.ReplyMarkupSpec> replyMarkup();

    Optional<Instant> scheduleTimestamp();
}
