package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.PeerId;
import telegram4j.core.util.EntityParserFactory;

import java.time.Instant;
import java.util.Optional;

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

    @Value.Default
    default boolean noForwards() {
        return false;
    }

    Optional<Integer> replyToMessageId();

    String message();

    Optional<EntityParserFactory> parser();

    // Full qualification is need due to immutables doesnt able to import spec
    Optional<telegram4j.core.spec.markup.ReplyMarkupSpec> replyMarkup();

    Optional<Instant> scheduleTimestamp();

    Optional<PeerId> sendAs();
}
