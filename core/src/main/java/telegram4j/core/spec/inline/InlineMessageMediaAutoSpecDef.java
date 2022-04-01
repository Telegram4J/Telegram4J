package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.core.util.EntityParserFactory;

import java.util.Optional;

@Value.Immutable
interface InlineMessageMediaAutoSpecDef extends InlineMessageSpec {

    String message();

    Optional<EntityParserFactory> parser();
}
