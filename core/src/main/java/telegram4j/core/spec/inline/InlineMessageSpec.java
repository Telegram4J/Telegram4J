package telegram4j.core.spec.inline;

import telegram4j.core.spec.MessageFields;
import telegram4j.core.spec.Spec;

import java.util.Optional;

public interface InlineMessageSpec extends Spec {

    Optional<MessageFields.ReplyMarkupSpec> replyMarkup();
}
