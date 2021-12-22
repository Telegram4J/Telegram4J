package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.TelegramObject;

import java.util.Objects;
import java.util.Optional;

public class KeyboardButton implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final Type type;
    private final String text;
    @Nullable
    private final byte[] data;
    @Nullable
    private final Boolean requiresPassword;
    @Nullable
    private final Boolean quiz;
    @Nullable
    private final String query;
    @Nullable
    private final String url;
    @Nullable
    private final String forwardText;
    @Nullable
    private final Integer buttonId;
    @Nullable
    private final Boolean requestWriteAccess;

    public KeyboardButton(MTProtoTelegramClient client, Type type, String text) {
        this(client, type, text, null, null, null,
                null, null, null, null, null);
    }

    public KeyboardButton(MTProtoTelegramClient client, Type type, String text,
                          @Nullable byte[] data, @Nullable Boolean requiresPassword,
                          @Nullable Boolean quiz, @Nullable String query, @Nullable String url,
                          @Nullable String forwardText, @Nullable Integer buttonId,
                          @Nullable Boolean requestWriteAccess) {
        this.client = Objects.requireNonNull(client, "client");
        this.type = Objects.requireNonNull(type, "type");
        this.text = Objects.requireNonNull(text, "text");
        this.data = data;
        this.requiresPassword = requiresPassword;
        this.quiz = quiz;
        this.query = query;
        this.url = url;
        this.forwardText = forwardText;
        this.buttonId = buttonId;
        this.requestWriteAccess = requestWriteAccess;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public Optional<byte[]> getData() {
        return Optional.ofNullable(data);
    }

    public Optional<Boolean> isQuiz() {
        return Optional.ofNullable(quiz);
    }

    public Optional<String> getQuery() {
        return Optional.ofNullable(query);
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public Optional<String> getForwardText() {
        return Optional.ofNullable(forwardText);
    }

    public Optional<Integer> getButtonId() {
        return Optional.ofNullable(buttonId);
    }

    public Optional<Boolean> isRequestWriteAccess() {
        return Optional.ofNullable(requestWriteAccess);
    }

    public Optional<Boolean> requiresPassword() {
        return Optional.ofNullable(requiresPassword);
    }

    // TODO: implement
    // public InputUser getBotId() {
    //     return data.bot();
    // }

    public enum Type {
        DEFAULT,

        BUY,

        INPUT_URH_AUTH,

        CALLBACK,

        GAME,

        REQUEST_GEO_LOCATION,

        REQUEST_PHONE,

        REQUEST_POLL,

        SWITCH_INLINE,

        URL,

        URL_AUTH,
    }
}
