package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.spec.Spec;
import telegram4j.tl.InputMedia;

public interface InputMediaSpec extends Spec {

    Mono<InputMedia> asData(MTProtoTelegramClient client);
}
