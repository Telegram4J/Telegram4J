package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableReplyKeyboardMarkupData.class)
@JsonDeserialize(as = ImmutableReplyKeyboardMarkupData.class)
public interface ReplyKeyboardMarkupData extends ReplyMarkup {

    static ImmutableReplyKeyboardMarkupData.Builder builder() {
        return ImmutableReplyKeyboardMarkupData.builder();
    }

    List<List<KeyboardButtonData>> keyboard();

    @JsonProperty("resize_keyboard")
    Optional<Boolean> resizeKeyboard();

    @JsonProperty("one_time_keyboard")
    Optional<Boolean> oneTimeKeyboard();

    @JsonProperty("input_field_placeholder")
    Optional<String> inputFieldPlaceholder();
}
