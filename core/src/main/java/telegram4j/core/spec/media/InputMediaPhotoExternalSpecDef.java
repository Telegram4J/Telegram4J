package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaPhotoExternal;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable(builder = false)
interface InputMediaPhotoExternalSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.PHOTO_EXTERNAL;
    }

    String url();

    Optional<Duration> autoDeleteDuration();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(InputMediaPhotoExternal.builder()
                .url(url())
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build());
    }
}
