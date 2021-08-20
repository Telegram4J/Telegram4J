package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableVoiceChatParticipantsInvitedData.class)
@JsonDeserialize(as = ImmutableVoiceChatParticipantsInvitedData.class)
public interface VoiceChatParticipantsInvitedData {

    static ImmutableVoiceChatParticipantsInvitedData.Builder builder() {
        return ImmutableVoiceChatParticipantsInvitedData.builder();
    }

    Optional<List<UserData>> users();
}
