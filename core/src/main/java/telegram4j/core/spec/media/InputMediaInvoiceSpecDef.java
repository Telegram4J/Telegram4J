package telegram4j.core.spec.media;

import io.netty.buffer.ByteBuf;
import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.*;

import java.util.Optional;

@Value.Immutable
interface InputMediaInvoiceSpecDef extends InputMediaSpec {

    String title();

    String description();

    // TODO
    Optional<InputWebDocument> photo();

    Invoice invoice();

    ByteBuf payload();

    String provider();

    String providerJsonData();

    Optional<String> startParam();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.just(InputMediaInvoice.builder()
                .title(title())
                .description(description())
                .photo(photo().orElse(null))
                .invoice(invoice())
                .payload(payload())
                .provider(provider())
                .providerData(ImmutableDataJSON.of(providerJsonData()))
                .startParam(startParam().orElse(null))
                .build());
    }
}
