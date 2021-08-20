package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableMessageData.class)
@JsonDeserialize(as = ImmutableMessageData.class)
public interface MessageData {

    static ImmutableMessageData.Builder builder() {
        return ImmutableMessageData.builder();
    }

    @JsonProperty("message_id")
    long messageId();

    // NOTE: renamed due matches to generated #from method
    @JsonProperty("from")
    Optional<UserData> fromUser();

    @JsonProperty("sender_chat")
    Optional<ChatData> senderChat();

    int date();

    ChatData chat();

    @JsonProperty("forward_from_user")
    Optional<UserData> forwardFromUser();

    @JsonProperty("forward_from_chat")
    Optional<ChatData> forwardFromChat();

    @JsonProperty("forward_from_message_id")
    Optional<Long> forwardFromMessageId();

    @JsonProperty("forward_signature")
    Optional<String> forwardSignature();

    @JsonProperty("forward_sender_name")
    Optional<String> forwardSenderName();

    @JsonProperty("forward_date")
    Optional<Integer> forwardDate();

    @JsonProperty("reply_to_message")
    Optional<MessageData> replyToMessage();

    @JsonProperty("via_bot")
    Optional<UserData> viaBot();

    @JsonProperty("edit_date")
    Optional<Integer> editDate();

    @JsonProperty("media_group_id")
    Optional<String> mediaGroupId();

    @JsonProperty("author_signature")
    Optional<String> authorSignature();

    Optional<String> text();

    Optional<List<MessageEntityData>> entities();

    Optional<AnimationData> animation();

    Optional<AudioData> audio();

    Optional<DocumentData> document();

    Optional<List<PhotoSizeData>> photo();

    Optional<StickerData> sticker();

    Optional<VideoData> video();

    @JsonProperty("video_note")
    Optional<VideoNoteData> videoNote();

    Optional<VoiceData> voice();

    Optional<String> caption();

    @JsonProperty("caption_entities")
    Optional<List<MessageEntityData>> captionEntities();

    Optional<ContactData> contact();

    Optional<DiceData> dice();

    Optional<GameData> game();

    Optional<PollData> poll();

    Optional<VenueData> venue();

    Optional<LocationData> location();

    @JsonProperty("new_chat_members")
    Optional<List<UserData>> newChatMembers();

    @JsonProperty("left_chat_member")
    Optional<UserData> leftChatMember();

    @JsonProperty("new_chat_title")
    Optional<String> newChatTitle();

    @JsonProperty("new_chat_photo")
    Optional<List<PhotoSizeData>> newChatPhoto();

    @JsonProperty("delete_chat_photo")
    Optional<Boolean> deleteChatPhoto();

    @JsonProperty("group_chat_created")
    Optional<Boolean> groupChatCreated();

    @JsonProperty("supergroup_chat_created")
    Optional<Boolean> supergroupChatCreated();

    @JsonProperty("channel_chat_created")
    Optional<Boolean> channelChatCreated();

    @JsonProperty("message_auto_delete_timer_changed")
    Optional<MessageAutoDeleteTimerChangedData> messageAutoDeleteTimerChanged();

    @JsonProperty("migrate_to_chat_id")
    Optional<Long> migrateToChatId();

    @JsonProperty("migrate_from_chat_id")
    Optional<Long> migrateFromChatId();

    @JsonProperty("pinned_message")
    Optional<MessageData> pinnedMessage();

    Optional<InvoiceData> invoice();

    @JsonProperty("successful_payment")
    Optional<SuccessfulPaymentData> successfulPayment();

    @JsonProperty("connected_website")
    Optional<String> connectedWebsite();

    @JsonProperty("passport_data")
    Optional<PassportData> passportData();

    @JsonProperty("proximity_alert_triggered")
    Optional<ProximityAlertTriggeredData> proximityAlertTriggered();

    @JsonProperty("voice_chat_scheduled")
    Optional<VoiceChatScheduledData> voiceChatScheduled();

    @JsonProperty("voice_chat_started")
    Optional<VoiceChatStartedData> voiceChatStarted();

    @JsonProperty("voice_chat_ended")
    Optional<VoiceChatEndedData> voiceChatEnded();

    @JsonProperty("voice_chat_participants_invited")
    Optional<VoiceChatParticipantsInvitedData> voiceChatParticipantsInvited();

    @JsonProperty("reply_markup")
    Optional<InlineKeyboardMarkupData> replyMarkup();
}
