package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutablePassportData.class)
@JsonDeserialize(as = ImmutablePassportData.class)
public interface PassportData {

    static ImmutablePassportData.Builder builder() {
        return ImmutablePassportData.builder();
    }

    List<EncryptedPassportElementData> data();

    EncryptedCredentialsData credentials();
}
