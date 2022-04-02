package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaPhotoExternal;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable
interface InputMediaPhotoExternalSpecDef extends InputMediaSpec {

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
