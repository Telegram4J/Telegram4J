package telegram4j.core.spec.markup;

import org.immutables.value.Value;
import reactor.util.annotation.Nullable;
import telegram4j.core.object.markup.KeyboardButton;

import java.util.Optional;

@Value.Immutable
abstract class ReplyButtonSpecDef implements KeyboardButtonSpec {

    public static ReplyButtonSpec text(String text) {
        return ReplyButtonSpec.of(KeyboardButton.Type.DEFAULT, text);
    }

    public static ReplyButtonSpec requestGeoLocation(String text) {
        return ReplyButtonSpec.of(KeyboardButton.Type.REQUEST_GEO_LOCATION, text);
    }

    public static ReplyButtonSpec requestPhone(String text) {
        return ReplyButtonSpec.of(KeyboardButton.Type.REQUEST_PHONE, text);
    }

    public static ReplyButtonSpec requestPoll(String text, @Nullable Boolean quiz) {
        return ReplyButtonSpec.of(KeyboardButton.Type.REQUEST_POLL, text)
                .withQuiz(Optional.ofNullable(quiz));
    }

    public abstract Optional<Boolean> quiz();
}
