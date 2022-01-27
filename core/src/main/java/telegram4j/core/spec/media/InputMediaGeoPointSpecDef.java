package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.BaseInputGeoPoint;
import telegram4j.tl.InputMediaGeoPoint;

import java.util.Optional;

@Value.Immutable(builder = false)
interface InputMediaGeoPointSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.GEO_POINT;
    }

    double latitide();

    double longtitude();

    Optional<Integer> accuracyRadius();

    @Override
    default InputMediaGeoPoint asData() {
        return InputMediaGeoPoint.builder()
                .geoPoint(BaseInputGeoPoint.builder()
                        .lat(latitide())
                        .longState(longtitude())
                        .accuracyRadius(accuracyRadius().orElse(null))
                        .build())
                .build();
    }
}
