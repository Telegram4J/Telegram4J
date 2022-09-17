package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReplyKeyboardMarkup extends ReplyMarkup {

    private final telegram4j.tl.ReplyKeyboardMarkup data;

    public ReplyKeyboardMarkup(MTProtoTelegramClient client, telegram4j.tl.ReplyKeyboardMarkup data) {
        super(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public Type getType() {
        return Type.KEYBOARD;
    }

    /**
     * Gets whether this keyboard is resizing after flipping device.
     *
     * @return {@code true} keyboard is resizing after flipping device.
     */
    public boolean isResize() {
        return data.resize();
    }

    /**
     * Gets whether when the keyboard becomes unavailable after pressing the button.
     *
     * @return {@code true} if keyboard becomes unavailable after pressing the button.
     */
    public boolean isSingleUse() {
        return data.singleUse();
    }

    /**
     * Gets whether this keyboard is only for specific users selected
     * via @mention in the {@link Message#getContent()} or via message reply.
     *
     * @return {@code true} if keyboard is shown only for a specific user.
     */
    public boolean isSelective() {
        return data.selective();
    }

    /**
     * Gets nested lists with reply {@link KeyboardButton buttons}.
     *
     * @return The nested with {@link KeyboardButton reply buttons}.
     */
    public List<List<KeyboardButton>> getRows() {
        return data.rows().stream()
                .map(r -> r.buttons().stream()
                        .map(b -> new KeyboardButton(client, b))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * Gets the text which will be displayed in the ui
     * input field when the keyboard is active, if present
     *
     * @return The text which will be displayed in the input field, if present.
     */
    public Optional<String> getPlaceholder() {
        return Optional.ofNullable(data.placeholder());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplyKeyboardMarkup that = (ReplyKeyboardMarkup) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ReplyKeyboardMarkup{" +
                "data=" + data +
                '}';
    }
}
