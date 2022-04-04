package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaPhoto;
import telegram4j.tl.InputMediaPhotoExternal;
import telegram4j.tl.InputPhoto;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable
interface InputMediaPhotoSpecDef extends InputMediaSpec {

    /**
     * @return The serialized {@link FileReferenceId} or url to web file.
     */
    String photo();

    Optional<Duration> autoDeleteDuration();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromCallable(() -> {
                    Integer ttlSeconds = autoDeleteDuration()
                            .map(Duration::getSeconds)
                            .map(Math::toIntExact)
                            .orElse(null);

                    try {
                        InputPhoto doc = FileReferenceId.deserialize(photo()).asInputPhoto();

                        return InputMediaPhoto.builder()
                                .id(doc)
                                .ttlSeconds(ttlSeconds)
                                .build();
                    } catch (IllegalArgumentException t) {
                        return InputMediaPhotoExternal.builder()
                                .url(photo())
                                .ttlSeconds(ttlSeconds)
                                .build();
                    }
                });
    }
}
