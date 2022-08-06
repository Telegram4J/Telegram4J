package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaVenue;

import java.util.Optional;

@Value.Immutable
interface InputMediaVenueSpecDef extends InputMediaSpec {

    double latitude();

    double longitude();

    Optional<Integer> accuracyRadius();

    String title();

    String address();

    String provider();

    String venueId();

    String venueType();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(InputMediaVenue.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitude())
                        .longitude(longitude())
                        .accuracyRadius(accuracyRadius().orElse(null))
                        .build())
                .title(title())
                .address(address())
                .provider(provider())
                .venueId(venueId())
                .venueType(venueType())
                .build());
    }
}
