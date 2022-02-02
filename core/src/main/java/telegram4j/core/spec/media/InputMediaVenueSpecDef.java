package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.InputMediaVenue;

import java.util.Optional;

@Value.Immutable(builder = false)
interface InputMediaVenueSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.VENUE;
    }

    double latitide();

    double longtitude();

    Optional<Integer> accuracyRadius();

    String title();

    String address();

    String provider();

    String venueId();

    String venueType();

    @Override
    default InputMediaVenue asData() {
        return InputMediaVenue.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitide())
                        .longState(longtitude())
                        .accuracyRadius(accuracyRadius().orElse(null))
                        .build())
                .title(title())
                .address(address())
                .provider(provider())
                .venueId(venueId())
                .venueType(venueType())
                .build();
    }
}