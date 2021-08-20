package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableUpdateData.class)
@JsonDeserialize(as = ImmutableUpdateData.class)
public interface UpdateData {

    static ImmutableUpdateData.Builder builder() {
        return ImmutableUpdateData.builder();
    }

    @JsonProperty("update_id")
    int updateId();

    Optional<MessageData> message();

    @JsonProperty("edited_message")
    Optional<MessageData> editedMessage();

    @JsonProperty("channel_post")
    Optional<MessageData> channelPost();

    @JsonProperty("edited_channel_post")
    Optional<MessageData> editedChannelPost();

    @JsonProperty("inline_query")
    Optional<InlineQueryData> inlineQuery();

    @JsonProperty("chosen_inline_result")
    Optional<ChosenInlineResultData> chosenInlineResult();

    @JsonProperty("callback_query")
    Optional<CallbackQueryData> callbackQuery();

    @JsonProperty("shipping_query")
    Optional<ShippingQueryData> shippingQuery();

    @JsonProperty("pre_checkout_query")
    Optional<PreCheckoutQueryData> preCheckoutQuery();

    Optional<PollData> poll();

    @JsonProperty("poll_answer")
    Optional<PollAnswerData> pollAnswer();

    @JsonProperty("my_chat_member")
    Optional<ChatMemberUpdatedData> myChatMember();

    @JsonProperty("chat_member")
    Optional<ChatMemberUpdatedData> chatMember();
}
