package telegram4j.core.spec.inline;

import org.immutables.value.Value;

@Value.Immutable
interface InlineResultGameSpecDef extends InlineResultSpec {

    String shortName();

    @Override
    String id();

    @Override
    InlineMessageSpec message();
}
