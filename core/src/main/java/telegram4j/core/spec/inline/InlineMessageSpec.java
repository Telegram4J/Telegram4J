package telegram4j.core.spec.inline;

import telegram4j.core.spec.Spec;
import telegram4j.core.spec.markup.ReplyMarkupSpec;

import java.util.Optional;

public interface InlineMessageSpec extends Spec {

    Optional<ReplyMarkupSpec> replyMarkup();
}
