package telegram4j.json;

import org.immutables.value.Value;

@Value.Immutable
public interface DiceData {

    static ImmutableDiceData.Builder builder() {
        return ImmutableDiceData.builder();
    }

    String emoji();

    int value();
}
