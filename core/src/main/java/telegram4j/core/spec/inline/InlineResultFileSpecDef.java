package telegram4j.core.spec.inline;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
interface InlineResultFileSpecDef extends InlineResultSpec {

    Optional<String> title();

    Optional<String> description();

    String file();

    Optional<String> mimeType();

    WebDocumentSpec thumb();
}
