package telegram4j.core.spec.inline;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
interface InlineResultArticleSpecDef extends InlineResultSpec {

    String title();

    Optional<String> description();

    String url();

    Optional<WebDocumentSpec> thumb();

    @Override
    String id();

    @Override
    InlineMessageSpec message();
}
