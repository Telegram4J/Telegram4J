package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.util.EntityParser;
import telegram4j.tl.InputMedia;
import telegram4j.tl.ReplyMarkup;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

@Value.Immutable(singleton = true)
public interface EditMessageSpecDef extends Spec {

    default boolean noWebpage() {
        return false;
    }

    Optional<String> message();

    Optional<InputMedia> media();

    Optional<ReplyMarkup> replyMarkup();

    // Optional<List<MessageEntity>> entities();

    Optional<Instant> scheduleTimestamp();

    Optional<Function<String, EntityParser>> parser();
}
