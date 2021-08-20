package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
public interface VenueData {

    static ImmutableVenueData.Builder builder() {
        return ImmutableVenueData.builder();
    }

    LocationData location();

    String title();

    String addresses();

    @JsonProperty("foursquare_id")
    String foursquareId();

    //TODO
    @JsonProperty("foursquare_type")
    Optional<String> foursquareType();

    @JsonProperty("google_place_id")
    Optional<String> googlePlaceId();

    @JsonProperty("google_place_type")
    String googlePlaceType();
}
