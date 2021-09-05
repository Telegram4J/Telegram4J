package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import telegram4j.json.ReplyMarkupData;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableStopPollRequest.class)
@JsonDeserialize(as = ImmutableStopPollRequest.class)
public interface StopPollRequest {

    static ImmutableStopPollRequest.Builder builder() {
        return ImmutableStopPollRequest.builder();
    }

    @JsonProperty("chat_id")
    ChatId chatId();

    @JsonProperty("message_id")
    Id messageId();

    @JsonProperty("reply_markup")
    Optional<ReplyMarkupData> replyMarkup();
}
