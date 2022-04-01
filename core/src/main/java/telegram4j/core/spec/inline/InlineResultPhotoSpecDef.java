package telegram4j.core.spec.inline;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
interface InlineResultPhotoSpecDef extends InlineResultSpec {

    Optional<String> title();

    Optional<String> description();

    String photo();

    Optional<SizeSpec> photoSize();

    Optional<WebDocumentSpec> thumb();
}
