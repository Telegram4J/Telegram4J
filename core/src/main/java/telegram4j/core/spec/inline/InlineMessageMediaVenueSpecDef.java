package telegram4j.core.spec.inline;

import org.immutables.value.Value;

@Value.Immutable
interface InlineMessageMediaVenueSpecDef extends InlineMessageSpec {

    telegram4j.core.spec.media.InputMediaVenueSpec media();
}
