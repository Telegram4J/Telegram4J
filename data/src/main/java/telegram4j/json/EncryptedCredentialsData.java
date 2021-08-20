package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableEncryptedCredentialsData.class)
@JsonDeserialize(as = ImmutableEncryptedCredentialsData.class)
public interface EncryptedCredentialsData {

    static ImmutableEncryptedCredentialsData.Builder builder() {
        return ImmutableEncryptedCredentialsData.builder();
    }

    String data();

    String hash();

    String secret();
}
