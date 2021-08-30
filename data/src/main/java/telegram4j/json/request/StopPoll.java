package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.InlineKeyboardMarkupData;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableStopPoll.class)
@JsonDeserialize(as = ImmutableStopPoll.class)
public interface StopPoll {

    static ImmutableStopPoll.Builder builder() {
        return ImmutableStopPoll.builder();
    }

    @JsonProperty("chat_id")
    long chatId();

    @JsonProperty("message_id")
    long messageId();

    @JsonProperty("reply_markup")
    Optional<InlineKeyboardMarkupData> replyMarkup();
}
