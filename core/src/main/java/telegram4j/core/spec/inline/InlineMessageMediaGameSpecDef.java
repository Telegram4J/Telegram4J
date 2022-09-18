package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.core.spec.markup.ReplyMarkupSpec;

import java.util.Optional;

@Value.Immutable
interface InlineMessageMediaGameSpecDef extends InlineMessageSpec {

    @Override
    Optional<ReplyMarkupSpec> replyMarkup();
}
