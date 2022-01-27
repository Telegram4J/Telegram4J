package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.ImmutableInputMediaDice;
import telegram4j.tl.InputMediaDice;

@Value.Immutable(builder = false)
interface InputMediaDiceSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.DICE;
    }

    String emoticon();

    @Override
    default InputMediaDice asData() {
        return ImmutableInputMediaDice.of(emoticon());
    }
}
