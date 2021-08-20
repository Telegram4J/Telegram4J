package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableContactData.class)
@JsonDeserialize(as = ImmutableContactData.class)
public interface ContactData {

    static ImmutableContactData.Builder builder() {
        return ImmutableContactData.builder();
    }

    @JsonProperty("phone_number")
    String phoneNumber();

    @JsonProperty("first_name")
    String firstName();

    @JsonProperty("last_name")
    Optional<String> lastName();

    @JsonProperty("user_id")
    Optional<Long> userId();

    Optional<String> vcard();
}
