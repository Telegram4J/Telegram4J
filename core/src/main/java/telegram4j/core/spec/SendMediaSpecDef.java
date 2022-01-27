package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.PeerId;
import telegram4j.core.spec.media.InputMediaSpec;
import telegram4j.core.util.EntityParser;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

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

    Optional<MessageFields.ReplyMarkupSpec> replyMarkup();

    Optional<Instant> scheduleTimestamp();

    Optional<Function<String, EntityParser>> parser();

    Optional<PeerId> sendAs();
}
