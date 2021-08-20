package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableUserData.class)
@JsonDeserialize(as = ImmutableUserData.class)
public interface UserData {

    static ImmutableUserData.Builder builder() {
        return ImmutableUserData.builder();
    }

    long id();

    @JsonProperty("is_bot")
    boolean isBot();

    @JsonProperty("first_name")
    Optional<String> firstName();

    @JsonProperty("last_name")
    Optional<String> lastName();

    Optional<String> username();

    @JsonProperty("language_code")
    Optional<String> languageCode();

    @JsonProperty("can_join_groups")
    Optional<Boolean> canJoinGroups();

    @JsonProperty("can_read_all_group_messages")
    Optional<Boolean> canReadAllGroupMessages();

    @JsonProperty("supports_inline_queries")
    Optional<Boolean> supportsInlineQueries();
}
