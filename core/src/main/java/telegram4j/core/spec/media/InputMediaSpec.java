package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputMedia;

// TODO: Implement spec for inputMediaGame, inputMediaPoll
public interface InputMediaSpec {

    Mono<InputMedia> asData(MTProtoTelegramClient client);
}
