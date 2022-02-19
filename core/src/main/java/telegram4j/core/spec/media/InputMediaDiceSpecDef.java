package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ImmutableInputMediaDice;
import telegram4j.tl.InputMedia;

@Value.Immutable(builder = false)
interface InputMediaDiceSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.DICE;
    }

    String emoticon();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(ImmutableInputMediaDice.of(emoticon()));
    }
}
