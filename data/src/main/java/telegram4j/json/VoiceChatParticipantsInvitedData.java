package telegram4j.json;

import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
public interface VoiceChatParticipantsInvitedData {

    static ImmutableVoiceChatParticipantsInvitedData.Builder builder() {
        return ImmutableVoiceChatParticipantsInvitedData.builder();
    }

    Optional<List<UserData>> users();
}
