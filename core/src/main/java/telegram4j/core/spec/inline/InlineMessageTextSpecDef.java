package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.util.parser.EntityParserFactory;

import java.util.Optional;

@Value.Immutable
interface InlineMessageTextSpecDef extends InlineMessageSpec {

    @Value.Default
    default boolean noWebpage() {
        return false;
    }

    String message();

    Optional<EntityParserFactory> parser();

    @Override
    Optional<ReplyMarkupSpec> replyMarkup();
}
