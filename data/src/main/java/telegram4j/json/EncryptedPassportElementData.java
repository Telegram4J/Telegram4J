package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface EncryptedPassportElementData {

    static ImmutableEncryptedPassportElementData.Builder builder() {
        return ImmutableEncryptedPassportElementData.builder();
    }

    EncryptedPassportElementType type();

    Optional<String> data();

    @JsonProperty("phone_number")
    Optional<String> phoneNumber();

    Optional<String> email();

    Optional<List<PassportFileData>> files();

    @JsonProperty("front_side")
    Optional<PassportFileData> frontSide();

    @JsonProperty("reverse_side")
    Optional<PassportFileData> reverseSide();

    Optional<PassportFileData> selfie();

    Optional<List<PassportFileData>> translation();

    String hash();
}
