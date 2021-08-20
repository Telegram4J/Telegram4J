package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableChatInviteLinkData.class)
@JsonDeserialize(as = ImmutableChatInviteLinkData.class)
public interface ChatInviteLinkData {

    static ImmutableChatInviteLinkData.Builder builder() {
        return ImmutableChatInviteLinkData.builder();
    }

    @JsonProperty("invite_link")
    String inviteLink();

    UserData creator();

    @JsonProperty("is_primary")
    boolean isPrimary();

    @JsonProperty("is_revoked")
    boolean isRevoked();

    @JsonProperty("expire_date")
    Optional<Integer> expireDate();

    @JsonProperty("member_limit")
    Optional<Integer> memberLimit();
}
