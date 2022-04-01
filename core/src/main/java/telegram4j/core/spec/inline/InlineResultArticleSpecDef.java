package telegram4j.core.spec.inline;

import org.immutables.value.Value;

@Value.Immutable
interface InlineResultArticleSpecDef extends InlineResultSpec {

    String title();

    String description();

    String url();

    WebDocumentSpec thumb();
}
