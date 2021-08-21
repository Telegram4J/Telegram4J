package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "status")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "owner", value = ChatMemberData.ChatMemberOwnerData.class),
        @JsonSubTypes.Type(name = "administrator", value = ChatMemberData.ChatMemberAdministratorData.class),
        @JsonSubTypes.Type(name = "member", value = ChatMemberData.ChatMemberMemberData.class),
        @JsonSubTypes.Type(name = "restricted", value = ChatMemberData.ChatMemberRestrictedData.class),
        @JsonSubTypes.Type(name = "left", value = ChatMemberData.ChatMemberLeftData.class),
        @JsonSubTypes.Type(name = "banned", value = ChatMemberData.ChatMemberBannedData.class)
})
public interface ChatMemberData {

    @JsonTypeId
    ChatMemberType status();

    UserData user();

    @Value.Immutable
    @JsonSerialize(as = ImmutableChatMemberOwnerData.class)
    @JsonDeserialize(as = ImmutableChatMemberOwnerData.class)
    interface ChatMemberOwnerData extends ChatMemberData {

        static ImmutableChatMemberOwnerData.Builder builder() {
            return ImmutableChatMemberOwnerData.builder();
        }

        @Override
        default ChatMemberType status() {
            return ChatMemberType.OWNER;
        }

        @JsonProperty("is_anonymous")
        boolean isAnonymous();

        @JsonProperty("custom_title")
        Optional<String> customTitle();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableChatMemberAdministratorData.class)
    @JsonDeserialize(as = ImmutableChatMemberAdministratorData.class)
    interface ChatMemberAdministratorData extends ChatMemberData {

        static ImmutableChatMemberAdministratorData.Builder builder() {
            return ImmutableChatMemberAdministratorData.builder();
        }

        @Override
        default ChatMemberType status() {
            return ChatMemberType.ADMINISTRATOR;
        }

        @JsonProperty("can_be_edited")
        boolean canBeEdited();

        @JsonProperty("is_anonymous")
        boolean isAnonymous();

        @JsonProperty("can_manage_chat")
        boolean canManageChat();

        @JsonProperty("can_delete_messages")
        boolean canDeleteMessages();

        @JsonProperty("can_manage_voice_chats")
        boolean canManageVoiceChats();

        @JsonProperty("can_restrict_members")
        boolean canRestrictMembers();

        @JsonProperty("can_promote_members")
        boolean canPromoteMembers();

        @JsonProperty("can_change_info")
        boolean canChangeInfo();

        @JsonProperty("can_invite_users")
        boolean canInviteUsers();

        @JsonProperty("can_post_messages")
        Optional<Boolean> canPostMessages();

        @JsonProperty("can_edit_messages")
        Optional<Boolean> canEditMessages();

        @JsonProperty("can_pin_messages")
        Optional<Boolean> canPinMessages();

        @JsonProperty("custom_title")
        Optional<String> customTitle();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableChatMemberMemberData.class)
    @JsonDeserialize(as = ImmutableChatMemberMemberData.class)
    interface ChatMemberMemberData extends ChatMemberData {

        static ImmutableChatMemberMemberData.Builder builder() {
            return ImmutableChatMemberMemberData.builder();
        }

        @Override
        default ChatMemberType status() {
            return ChatMemberType.MEMBER;
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableChatMemberRestrictedData.class)
    @JsonDeserialize(as = ImmutableChatMemberRestrictedData.class)
    interface ChatMemberRestrictedData extends ChatMemberData {

        static ImmutableChatMemberRestrictedData.Builder builder() {
            return ImmutableChatMemberRestrictedData.builder();
        }

        @Override
        default ChatMemberType status() {
            return ChatMemberType.RESTRICTED;
        }

        @JsonProperty("is_member")
        boolean isMember();

        @JsonProperty("can_change_info")
        boolean canChangeInfo();

        @JsonProperty("can_invite_users")
        boolean canInviteUsers();

        @JsonProperty("can_pin_messages")
        boolean canPinMessages();

        @JsonProperty("can_send_media_messages")
        boolean canSendMediaMessages();

        @JsonProperty("can_send_polls")
        boolean canSendPolls();

        @JsonProperty("can_send_other_messages")
        boolean canSendOtherMessages();

        @JsonProperty("can_add_web_page_previews")
        boolean canAddWebPagePreviews();

        @JsonProperty("until_date")
        int untilDate();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableChatMemberLeftData.class)
    @JsonDeserialize(as = ImmutableChatMemberLeftData.class)
    interface ChatMemberLeftData extends ChatMemberData {

        static ImmutableChatMemberLeftData.Builder builder() {
            return ImmutableChatMemberLeftData.builder();
        }

        @Override
        default ChatMemberType status() {
            return ChatMemberType.LEFT;
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableChatMemberBannedData.class)
    @JsonDeserialize(as = ImmutableChatMemberBannedData.class)
    interface ChatMemberBannedData extends ChatMemberData {

        static ImmutableChatMemberBannedData.Builder builder() {
            return ImmutableChatMemberBannedData.builder();
        }

        @Override
        default ChatMemberType status() {
            return ChatMemberType.BANNED;
        }

        @JsonProperty("until_date")
        int untilDate();
    }
}
