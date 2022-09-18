package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.util.parser.EntityParserFactory;

import java.util.Optional;

/**
 * Inline message with media from underling {@link InlineResultSpec}.
 */
@Value.Immutable
interface InlineMessageMediaAutoSpecDef extends InlineMessageSpec {

    String message();

    Optional<EntityParserFactory> parser();

    @Override
    Optional<ReplyMarkupSpec> replyMarkup();
}
