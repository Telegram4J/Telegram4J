package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaGeoPoint;

import java.util.Optional;

@Value.Immutable
interface InputMediaGeoPointSpecDef extends InputMediaSpec {

    double latitide();

    double longtitude();

    Optional<Integer> accuracyRadius();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(InputMediaGeoPoint.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitide())
                        .longState(longtitude())
                        .accuracyRadius(accuracyRadius().orElse(null))
                        .build())
                .build());
    }
}
