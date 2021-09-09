package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable(singleton = true)
@JsonSerialize(as = ImmutableReplyMarkupData.class)
@JsonDeserialize(as = ImmutableReplyMarkupData.class)
public interface ReplyMarkupData {

    static ImmutableReplyMarkupData.Builder builder() {
        return ImmutableReplyMarkupData.builder();
    }

    @JsonProperty("force_reply")
    Optional<Boolean> forceReply();

    @JsonProperty("input_field_placeholder")
    Optional<String> inputFieldPlaceholder();

    Optional<Boolean> selective();

    Optional<List<List<KeyboardButtonData>>> keyboard();

    @JsonProperty("resize_keyboard")
    Optional<Boolean> resizeKeyboard();

    @JsonProperty("one_time_keyboard")
    Optional<Boolean> oneTimeKeyboard();

    @JsonProperty("remove_keyboard")
    Optional<Boolean> removeKeyboard();

    @JsonProperty("inline_keyboard")
    List<InlineKeyboardButtonData> inlineKeyboard();
}
