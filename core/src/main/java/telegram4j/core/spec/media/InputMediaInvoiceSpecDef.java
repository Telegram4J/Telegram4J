package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.ImmutableDataJSON;
import telegram4j.tl.InputMediaInvoice;
import telegram4j.tl.InputWebDocument;
import telegram4j.tl.Invoice;

import java.util.Optional;

// TODO too raw mapping
@Value.Immutable(builder = false)
interface InputMediaInvoiceSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.INVOICE;
    }

    String title();

    String description();

    Optional<InputWebDocument> photo();

    Invoice invoice();

    byte[] payload();

    String provider();

    String providerJsonData();

    Optional<String> startParam();

    @Override
    default InputMediaInvoice asData() {
        return InputMediaInvoice.builder()
                .title(title())
                .description(description())
                .photo(photo().orElse(null))
                .invoice(invoice())
                .payload(payload())
                .provider(provider())
                .providerData(ImmutableDataJSON.of(providerJsonData()))
                .startParam(startParam().orElse(null))
                .build();
    }
}
