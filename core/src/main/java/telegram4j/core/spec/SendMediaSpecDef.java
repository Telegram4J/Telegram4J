package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.spec.media.InputMediaSpec;
import telegram4j.core.util.PeerId;
import telegram4j.core.util.parser.EntityParserFactory;

import java.time.Instant;
import java.util.Optional;

@Value.Immutable
interface SendMediaSpecDef extends Spec {

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

    InputMediaSpec media();

    String message();

    Optional<telegram4j.core.spec.markup.ReplyMarkupSpec> replyMarkup();

    Optional<Instant> scheduleTimestamp();

    Optional<EntityParserFactory> parser();

    Optional<PeerId> sendAs();
}
