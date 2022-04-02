package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.mtproto.file.FileReferenceId;

import java.util.Optional;

@Value.Immutable
interface InlineResultPhotoSpecDef extends InlineResultSpec {

    /**
     * @return The serialized {@link FileReferenceId} of photo or URL.
     */
    String photo();

    Optional<SizeSpec> photoSize();

    Optional<WebDocumentSpec> thumb();
}
