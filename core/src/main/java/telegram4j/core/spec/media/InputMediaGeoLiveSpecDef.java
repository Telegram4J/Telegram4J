package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.InputMediaGeoLive;

import java.util.Optional;

@Value.Immutable(builder = false)
interface InputMediaGeoLiveSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.GEO_LIVE;
    }

    @Value.Default
    default boolean stopped() {
        return false;
    }

    double latitide();

    double longtitude();

    Optional<Integer> accuracyRadius();

    Optional<Integer> heading();

    Optional<Integer> period();

    Optional<Integer> proximityNotificationRadius();

    @Override
    default InputMediaGeoLive asData() {
        return InputMediaGeoLive.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitide())
                        .longState(longtitude())
                        .accuracyRadius(accuracyRadius().orElse(null))
                        .build())
                .heading(heading().orElse(null))
                .heading(period().orElse(null))
                .heading(proximityNotificationRadius().orElse(null))
                .build();
    }
}
