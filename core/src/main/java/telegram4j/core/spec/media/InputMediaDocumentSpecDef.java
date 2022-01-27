package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputMediaDocument;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable(builder = false)
interface InputMediaDocumentSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.DOCUMENT;
    }

    String document();

    Optional<String> query();

    Optional<Duration> autoDeleteDuration();

    @Override
    default InputMediaDocument asData() {
        FileReferenceId fileReferenceId = FileReferenceId.deserialize(document());

        return InputMediaDocument.builder()
                .id(fileReferenceId.asInputDocument())
                .query(query().orElse(null))
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build();
    }
}
