package telegram4j.core.spec.inline;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
interface InlineResultPhotoSpecDef extends InlineResultSpec {

    /**
     * @return The serialized {@link telegram4j.mtproto.file.FileReferenceId} of photo or URL.
     */
    String photo();

    Optional<SizeSpec> photoSize();

    Optional<WebDocumentSpec> thumb();
}
