package telegram4j.core.spec.inline;

import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
interface InlineMessageMediaVenueSpecDef extends InlineMessageSpec {

    double latitide();

    double longtitude();

    Optional<Integer> accuracyRadius();

    String title();

    String address();

    String provider();

    String venueId();

    String venueType();
}
