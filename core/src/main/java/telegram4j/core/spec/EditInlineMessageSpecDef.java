package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.spec.media.InputMediaSpec;
import telegram4j.core.util.parser.EntityParser;

import java.util.Optional;
import java.util.function.Function;

@Value.Immutable(singleton = true)
interface EditInlineMessageSpecDef extends Spec {

    @Value.Default
    default boolean noWebpage() {
        return false;
    }

    Optional<String> message();

    Optional<InputMediaSpec> media();

    Optional<telegram4j.core.spec.markup.ReplyMarkupSpec> replyMarkup();

    Optional<Function<String, EntityParser>> parser();
}
