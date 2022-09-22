package telegram4j.core.spec.inline;

import org.immutables.value.Value;
import telegram4j.mtproto.file.FileReferenceId.DocumentType;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable
interface InlineResultDocumentSpecDef extends InlineResultSpec {

    /**
     * @return The type of web file, if absent {@link telegram4j.mtproto.file.FileReferenceId.DocumentType#GENERAL}
     * would be used.
     */
    Optional<DocumentType> type();

    String title();

    Optional<String> description();

    Optional<Duration> duration();

    Optional<SizeSpec> size();

    /**
     * @return The serialized {@link telegram4j.mtproto.file.FileReferenceId} of file or URL.
     */
    String file();

    /**
     * @return The mime type for web file. Must be <b>application/pdf</b> or <b>application/zip</b>
     */
    Optional<String> mimeType();

    Optional<WebDocumentSpec> thumb();

    @Override
    String id();

    @Override
    InlineMessageSpec message();
}
