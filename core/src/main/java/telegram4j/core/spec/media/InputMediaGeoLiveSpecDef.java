package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaGeoLive;

import java.util.Optional;

@Value.Immutable
interface InputMediaGeoLiveSpecDef extends InputMediaSpec {

    @Value.Default
    default boolean stopped() {
        return false;
    }

    double latitude();

    double longitude();

    Optional<Integer> accuracyRadius();

    Optional<Integer> heading();

    Optional<Integer> period();

    Optional<Integer> proximityNotificationRadius();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(InputMediaGeoLive.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitude())
                        .longitude(longitude())
                        .accuracyRadius(accuracyRadius().orElse(null))
                        .build())
                .heading(heading().orElse(null))
                .heading(period().orElse(null))
                .heading(proximityNotificationRadius().orElse(null))
                .build());
    }
}
