package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ImmutableInputMediaGame;
import telegram4j.tl.InputGame;
import telegram4j.tl.InputMedia;

@Value.Immutable
interface InputMediaGameSpecDef extends InputMediaSpec {

    InputGame game();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(ImmutableInputMediaGame.of(game()));
    }
}
