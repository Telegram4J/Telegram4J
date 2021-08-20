package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable(singleton = true) // because all fields are optional
@JsonSerialize(as = ImmutableChatPermissionsData.class)
@JsonDeserialize(as = ImmutableChatPermissionsData.class)
public interface ChatPermissionsData {

    static ImmutableChatPermissionsData.Builder builder() {
        return ImmutableChatPermissionsData.builder();
    }

    @JsonProperty("can_send_messages")
    Optional<Boolean> canSendMessages();

    @JsonProperty("can_send_media_messages")
    Optional<Boolean> canSendMediaMessages();

    @JsonProperty("can_send_polls")
    Optional<Boolean> canSendPolls();

    @JsonProperty("can_send_other_messages")
    Optional<Boolean> canSendOtherMessages();

    @JsonProperty("can_add_web_page_previews")
    Optional<Boolean> canAddWebPagePreviews();

    @JsonProperty("can_change_info")
    Optional<Boolean> canChangeInfo();

    @JsonProperty("can_invite_users")
    Optional<Boolean> canInviteUsers();

    @JsonProperty("can_pin_messages")
    Optional<Boolean> canPinMessages();
}
