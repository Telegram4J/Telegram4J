package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.ImmutableInputMediaGame;
import telegram4j.tl.InputGame;
import telegram4j.tl.InputMediaGame;

@Value.Immutable(builder = false)
interface InputMediaGameSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.GAME;
    }

    // TODO: unwrap?
    InputGame game();

    @Override
    default InputMediaGame asData() {
        return ImmutableInputMediaGame.of(game());
    }
}
