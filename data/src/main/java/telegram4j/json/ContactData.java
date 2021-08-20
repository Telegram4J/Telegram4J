package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
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
    Optional<Integer> userId();

    Optional<String> vcard();
}
