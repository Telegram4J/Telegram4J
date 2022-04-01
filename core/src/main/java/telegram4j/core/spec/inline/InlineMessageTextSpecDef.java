package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.core.util.EntityParserFactory;

import java.util.Optional;

@Value.Immutable
interface InlineMessageTextSpecDef extends InlineMessageSpec {

    @Value.Default
    default boolean noWebpage() {
        return false;
    }

    String message();

    Optional<EntityParserFactory> parser();
}
