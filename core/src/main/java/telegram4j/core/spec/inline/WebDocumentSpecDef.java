package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.core.spec.Spec;

import java.util.Optional;

@Value.Immutable
interface WebDocumentSpecDef extends Spec {

    String url();

    Optional<SizeSpec> imageSize();

    Optional<Integer> height();

    Optional<Integer> size();

    Optional<String> mimeType();
}

@Value.Immutable
interface SizeSpecDef extends Spec {

    int width();

    int height();
}
