package telegram4j.core.spec.markup;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.core.object.markup.KeyboardButton;

import java.util.Objects;
import java.util.Optional;

public final class ReplyButtonSpec implements KeyboardButtonSpec {
    private final KeyboardButton.Type type;
    private final String text;
    @Nullable
    private final Boolean quiz;
    private final int buttonId;
    private final RequestPeerSpec requestPeer;

    ReplyButtonSpec(KeyboardButton.Type type, String text) {
        this.type = Objects.requireNonNull(type);
        this.text = Objects.requireNonNull(text);
        this.quiz = null;
        this.requestPeer = null;
        this.buttonId = -1;
    }

    ReplyButtonSpec(KeyboardButton.Type type, String text, @Nullable Boolean quiz,
                    int buttonId, @Nullable RequestPeerSpec requestPeer) {
        this.type = type;
        this.text = text;
        this.quiz = quiz;
        this.requestPeer = requestPeer;
        this.buttonId = buttonId;
    }

    @Override
    public KeyboardButton.Type type() {
        return type;
    }

    @Override
    public String text() {
        return text;
    }

    public Optional<Integer> buttonId() {
        return type == KeyboardButton.Type.REQUEST_PEER ? Optional.of(buttonId) : Optional.empty();
    }

    public Optional<RequestPeerSpec> requestPeer() {
        return Optional.ofNullable(requestPeer);
    }

    public Optional<Boolean> quiz() {
        return Optional.ofNullable(quiz);
    }

    public ReplyButtonSpec withQuiz(@Nullable Boolean value) {
        Preconditions.requireState(type == KeyboardButton.Type.REQUEST_POLL, () -> "unexpected type: " + type);
        if (Objects.equals(quiz, value)) return this;
        return new ReplyButtonSpec(type, text, value, buttonId, requestPeer);
    }

    public ReplyButtonSpec withQuiz(Optional<Boolean> opt) {
        return withQuiz(opt.orElse(null));
    }

    public ReplyButtonSpec withButtonId(int buttonId) {
        Preconditions.requireState(type == KeyboardButton.Type.REQUEST_PEER, () -> "unexpected type: " + type);
        if (this.buttonId == buttonId) return this;
        return new ReplyButtonSpec(type, text, quiz, buttonId, requestPeer);
    }

    public ReplyButtonSpec withRequestPeer(@Nullable RequestPeerSpec requestPeer) {
        Preconditions.requireState(type == KeyboardButton.Type.REQUEST_PEER, () -> "unexpected type: " + type);
        if (Objects.equals(this.requestPeer, requestPeer)) return this;
        return new ReplyButtonSpec(type, text, quiz, buttonId, requestPeer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplyButtonSpec that)) return false;
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

    public static ReplyButtonSpec requestPeer(String text, int buttonId, RequestPeerSpec requestPeer) {
        return new ReplyButtonSpec(KeyboardButton.Type.REQUEST_PEER, text)
                .withButtonId(buttonId)
                .withRequestPeer(requestPeer);
    }
}
