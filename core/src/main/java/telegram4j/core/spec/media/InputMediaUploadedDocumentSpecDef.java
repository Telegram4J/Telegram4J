package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.DocumentAttribute;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputMediaUploadedDocument;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable(builder = false)
interface InputMediaUploadedDocumentSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.UPLOADED_DOCUMENT;
    }

    @Value.Default
    default boolean noSoundVideo() {
        return false;
    }

    @Value.Default
    default boolean forceFile() {
        return false;
    }

    InputFile file();

    Optional<InputFile> thumb();

    String mimeType();

    List<DocumentAttribute> attributes();

    Optional<List<String>> stickers();

    Optional<Duration> autoDeleteDuration();

    @Override
    default InputMediaUploadedDocument asData() {
        var stickers = stickers()
                .map(list -> list.stream()
                        .map(s -> FileReferenceId.deserialize(s)
                                .asInputDocument())
                        .collect(Collectors.toList()))
                .orElse(null);

        return InputMediaUploadedDocument.builder()
                .nosoundVideo(noSoundVideo())
                .forceFile(forceFile())
                .file(file())
                .thumb(thumb().orElse(null))
                .mimeType(mimeType())
                .attributes(attributes())
                .stickers(stickers)
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build();
    }
}
