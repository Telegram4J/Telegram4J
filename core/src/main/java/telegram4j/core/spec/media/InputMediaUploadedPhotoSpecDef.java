package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaUploadedPhoto;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface InputMediaUploadedPhotoSpecDef extends InputMediaSpec {

    InputFile file();

    Optional<List<String>> stickers();

    Optional<Duration> autoDeleteDuration();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromCallable(() -> stickers()
                        .map(list -> list.stream()
                                .map(s -> FileReferenceId.deserialize(s).asInputDocument())
                                .collect(Collectors.toList()))
                        .orElse(null))
                .map(stickers -> InputMediaUploadedPhoto.builder()
                        .file(file())
                        .stickers(stickers)
                        .ttlSeconds(autoDeleteDuration()
                                .map(Duration::getSeconds)
                                .map(Math::toIntExact)
                                .orElse(null))
                        .build());
    }
}
