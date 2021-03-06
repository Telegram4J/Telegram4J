package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.DocumentAttribute;
import telegram4j.tl.InputFile;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaUploadedDocument;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface InputMediaUploadedDocumentSpecDef extends InputMediaSpec {

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
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromCallable(() -> stickers()
                        .map(list -> list.stream()
                                .map(s -> FileReferenceId.deserialize(s)
                                        .asInputDocument())
                                .collect(Collectors.toList()))
                        .orElse(null))
                .map(stickers -> InputMediaUploadedDocument.builder()
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
                        .build());
    }
}
