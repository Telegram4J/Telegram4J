package telegram4j.core.spec.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.internal.Preconditions;
import telegram4j.core.object.markup.KeyboardButton;

import java.util.Objects;
import java.util.Optional;

public final class ReplyButtonSpec implements KeyboardButtonSpec {
    private final KeyboardButton.Type type;
    private final String text;
    @Nullable
    private final Boolean quiz;

    ReplyButtonSpec(KeyboardButton.Type type, String text) {
        this.type = Objects.requireNonNull(type);
        this.text = Objects.requireNonNull(text);
        this.quiz = null;
    }

    private ReplyButtonSpec(KeyboardButton.Type type, String text, @Nullable Boolean quiz) {
        this.type = type;
        this.text = text;
        this.quiz = quiz;
    }

    @Override
    public KeyboardButton.Type type() {
        return type;
    }

    @Override
    public String text() {
        return text;
    }

    public Optional<Boolean> quiz() {
        return Optional.ofNullable(quiz);
    }

    public ReplyButtonSpec withQuiz(@Nullable Boolean value) {
        Preconditions.requireState(type == KeyboardButton.Type.REQUEST_POLL, () -> "unexpected type: " + type);
        if (Objects.equals(quiz, value)) return this;
        return new ReplyButtonSpec(type, text, value);
    }

    public ReplyButtonSpec withQuiz(Optional<Boolean> opt) {
        return withQuiz(opt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplyButtonSpec)) return false;
        ReplyButtonSpec that = (ReplyButtonSpec) o;
        return type.equals(that.type)
                && text.equals(that.text)
                && Objects.equals(quiz, that.quiz);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + text.hashCode();
        h += (h << 5) + Objects.hashCode(quiz);
        return h;
    }

    @Override
    public String toString() {
        return "ReplyButtonSpec{" +
                "type=" + type +
                ", text='" + text + '\'' +
                ", quiz=" + quiz +
                '}';
    }

    public static ReplyButtonSpec text(String text) {
        return new ReplyButtonSpec(KeyboardButton.Type.DEFAULT, text);
    }

    public static ReplyButtonSpec requestGeoLocation(String text) {
        return new ReplyButtonSpec(KeyboardButton.Type.REQUEST_GEO_LOCATION, text);
    }

    public static ReplyButtonSpec requestPhone(String text) {
        return new ReplyButtonSpec(KeyboardButton.Type.REQUEST_PHONE, text);
    }

    public static ReplyButtonSpec requestPoll(String text, @Nullable Boolean quiz) {
        return new ReplyButtonSpec(KeyboardButton.Type.REQUEST_POLL, text)
                .withQuiz(quiz);
    }
}
