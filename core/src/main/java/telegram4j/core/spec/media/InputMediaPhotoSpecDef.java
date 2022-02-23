package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaPhoto;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable(builder = false)
interface InputMediaPhotoSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.PHOTO;
    }

    String photo();

    Optional<Duration> autoDeleteDuration();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(photo()).asInputPhoto())
                .map(doc -> InputMediaPhoto.builder()
                        .id(doc)
                        .ttlSeconds(autoDeleteDuration()
                                .map(Duration::getSeconds)
                                .map(Math::toIntExact)
                                .orElse(null))
                        .build());
    }
}
