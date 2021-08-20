package telegram4j.json;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface PassportData {

    static ImmutablePassportData.Builder builder() {
        return ImmutablePassportData.builder();
    }

    List<EncryptedPassportElementData> data();

    EncryptedCredentialsData credentials();
}
