package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableInlineKeyboardMarkupData.class)
@JsonDeserialize(as = ImmutableInlineKeyboardMarkupData.class)
public interface InlineKeyboardMarkupData extends ReplyMarkup {

    static ImmutableInlineKeyboardMarkupData.Builder builder() {
        return ImmutableInlineKeyboardMarkupData.builder();
    }

    @JsonProperty("inline_keyboard")
    List<InlineKeyboardButtonData> inlineKeyboard();
}
