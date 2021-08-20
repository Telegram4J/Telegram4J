package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableBotCommandData.class)
@JsonDeserialize(as = ImmutableBotCommandData.class)
public interface BotCommandData {

    static ImmutableBotCommandData.Builder builder() {
        return ImmutableBotCommandData.builder();
    }

    String command();

    String description();
}
