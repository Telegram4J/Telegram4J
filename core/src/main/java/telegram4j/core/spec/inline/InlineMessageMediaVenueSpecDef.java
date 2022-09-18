package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.core.spec.markup.ReplyMarkupSpec;

import java.util.Optional;

@Value.Immutable
interface InlineMessageMediaVenueSpecDef extends InlineMessageSpec {

    telegram4j.core.spec.media.InputMediaVenueSpec media();

    @Override
    Optional<ReplyMarkupSpec> replyMarkup();
}
