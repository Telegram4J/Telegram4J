package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
public interface InlineKeyboardMarkupData {

    static ImmutableInlineKeyboardMarkupData.Builder builder() {
        return ImmutableInlineKeyboardMarkupData.builder();
    }

    @JsonProperty("inline_keyboard")
    List<InlineKeyboardButtonData> inlineKeyboard();
}
