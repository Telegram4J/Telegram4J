package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableChatData.class)
@JsonDeserialize(as = ImmutableChatData.class)
public interface ChatData {

    static ImmutableChatData.Builder builder() {
        return ImmutableChatData.builder();
    }

    long id();

    ChatType type();

    Optional<String> title();

    Optional<String> username();

    @JsonProperty("first_name")
    Optional<String> firstName();

    @JsonProperty("last_name")
    Optional<String> lastName();

    Optional<ChatPhotoData> photo();

    Optional<String> bio();

    Optional<String> description();

    @JsonProperty("invite_link")
    Optional<String> inviteLink();

    @JsonProperty("message_data")
    Optional<MessageData> pinnedMessage();

    Optional<ChatPermissionsData> permissions();

    @JsonProperty("slow_mode_delay")
    Optional<Integer> slowModeDelay();

    @JsonProperty("message_auto_delete_time")
    Optional<Integer> messageAutoDeleteTime();

    @JsonProperty("sticker_set_name")
    Optional<String> stickerSetName();

    @JsonProperty("can_set_sticker_set")
    Optional<Boolean> canSetStickerSet();

    @JsonProperty("linked_chat_id")
    Optional<Long> linkedChatId();

    Optional<ChatLocationData> location();
}
