package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaContact;

@Value.Immutable
interface InputMediaContactSpecDef extends InputMediaSpec {

    String phoneNumber();

    String firstName();

    String lastName();

    String vcard();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(InputMediaContact.builder()
                .phoneNumber(phoneNumber())
                .firstName(firstName())
                .lastName(lastName())
                .vcard(vcard())
                .build());
    }
}
