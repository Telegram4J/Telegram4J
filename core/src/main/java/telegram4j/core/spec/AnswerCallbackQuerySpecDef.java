package telegram4j.core.spec;

import org.immutables.value.Value;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable
interface AnswerCallbackQuerySpecDef extends Spec {

    @Value.Default
    default boolean alert() {
        return false;
    }

    Optional<String> message();

    Optional<String> url();

    Duration cacheTime();
}
