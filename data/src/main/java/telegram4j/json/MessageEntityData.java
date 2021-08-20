package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageEntityData.class)
@JsonDeserialize(as = ImmutableMessageEntityData.class)
public interface MessageEntityData {

    static ImmutableMessageEntityData.Builder builder() {
        return ImmutableMessageEntityData.builder();
    }

    MessageEntityType type();

    int offset();

    int length();

    Optional<String> url();

    Optional<UserData> user();

    Optional<String> language();
}
