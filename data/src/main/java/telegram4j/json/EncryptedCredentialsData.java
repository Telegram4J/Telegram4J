package telegram4j.json;

import org.immutables.value.Value;

@Value.Immutable
public interface EncryptedCredentialsData {

    static ImmutableEncryptedCredentialsData.Builder builder() {
        return ImmutableEncryptedCredentialsData.builder();
    }

    String data();

    String hash();

    String secret();
}
