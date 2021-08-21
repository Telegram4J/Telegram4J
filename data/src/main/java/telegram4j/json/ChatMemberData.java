package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableChatMemberData.class)
@JsonDeserialize(as = ImmutableChatMemberData.class)
public interface ChatMemberData {

    static ImmutableChatMemberData.Builder builder() {
        return ImmutableChatMemberData.builder();
    }

    ChatMemberType status();

    UserData user();

    @JsonProperty("is_anonymous")
    Optional<Boolean> isAnonymous();

    @JsonProperty("custom_title")
    Optional<String> customTitle();

    @JsonProperty("can_be_edited")
    Optional<Boolean> canBeEdited();

    @JsonProperty("can_manage_chat")
    Optional<Boolean> canManageChat();

    @JsonProperty("can_delete_messages")
    Optional<Boolean> canDeleteMessages();

    @JsonProperty("can_manage_voice_chats")
    Optional<Boolean> canManageVoiceChats();

    @JsonProperty("can_restrict_members")
    Optional<Boolean> canRestrictMembers();

    @JsonProperty("can_promote_members")
    Optional<Boolean> canPromoteMembers();

    @JsonProperty("can_change_info")
    Optional<Boolean> canChangeInfo();

    @JsonProperty("can_invite_users")
    Optional<Boolean> canInviteUsers();

    @JsonProperty("can_post_messages")
    Optional<Boolean> canPostMessages();

    @JsonProperty("can_edit_messages")
    Optional<Boolean> canEditMessages();

    @JsonProperty("can_pin_messages")
    Optional<Boolean> canPinMessages();

    @JsonProperty("is_member")
    Optional<Boolean> isMember();

    @JsonProperty("can_send_media_messages")
    Optional<Boolean> canSendMediaMessages();

    @JsonProperty("can_send_polls")
    Optional<Boolean> canSendPolls();

    @JsonProperty("can_send_other_messages")
    Optional<Boolean> canSendOtherMessages();

    @JsonProperty("can_add_web_page_previews")
    Optional<Boolean> canAddWebPagePreviews();

    @JsonProperty("until_date")
    Optional<Integer> untilDate();
}
