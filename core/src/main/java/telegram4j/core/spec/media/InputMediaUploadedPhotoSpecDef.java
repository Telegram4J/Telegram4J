package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputMediaUploadedPhoto;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable(builder = false)
interface InputMediaUploadedPhotoSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.UPLOADED_PHOTO;
    }

    InputFile file();

    Optional<List<String>> stickers();

    Optional<Duration> autoDeleteDuration();

    @Override
    default InputMediaUploadedPhoto asData() {
        var stickers = stickers()
                .map(list -> list.stream()
                        .map(s -> FileReferenceId.deserialize(s).asInputDocument())
                        .collect(Collectors.toList()))
                .orElse(null);

        return InputMediaUploadedPhoto.builder()
                .file(file())
                .stickers(stickers)
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build();
    }
}
