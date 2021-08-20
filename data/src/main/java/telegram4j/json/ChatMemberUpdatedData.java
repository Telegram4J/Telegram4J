package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableChatMemberUpdatedData.class)
@JsonDeserialize(as = ImmutableChatMemberUpdatedData.class)
public interface ChatMemberUpdatedData {

    static ImmutableChatMemberUpdatedData.Builder builder() {
        return ImmutableChatMemberUpdatedData.builder();
    }

    ChatData chat();

    // NOTE: renamed due matches to generated #from method
    @JsonProperty("from")
    UserData fromUser();

    int date();

    @JsonProperty("old_chat_member")
    ChatMemberData oldChatMember();

    @JsonProperty("new_chat_member")
    ChatMemberData newChatMember();

    @JsonProperty("invite_link")
    Optional<ChatInviteLinkData> inviteLink();
}
