package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableReplyKeyboardRemoveData.class)
@JsonDeserialize(as = ImmutableReplyKeyboardRemoveData.class)
public interface ReplyKeyboardRemoveData {

    static ImmutableReplyKeyboardRemoveData.Builder builder() {
        return ImmutableReplyKeyboardRemoveData.builder();
    }

    @JsonProperty("remove_keyboard")
    boolean removeKeyboard();

    Optional<Boolean> selective();
}
