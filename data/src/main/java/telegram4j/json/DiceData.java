package telegram4j.json;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableDiceData.class)
@JsonDeserialize(as = ImmutableDiceData.class)
public interface DiceData {

    static ImmutableDiceData.Builder builder() {
        return ImmutableDiceData.builder();
    }

    String emoji();

    int value();
}
