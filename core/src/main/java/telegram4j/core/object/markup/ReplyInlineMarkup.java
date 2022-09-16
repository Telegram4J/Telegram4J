package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Representation of inline keyboard.
 *
 * <a href="https://core.telegram.org/bots#inline-keyboards-and-on-the-fly-updating">Inline Keyboards</a>
 */
public final class ReplyInlineMarkup extends ReplyMarkup {

    private final telegram4j.tl.ReplyInlineMarkup data;

    public ReplyInlineMarkup(MTProtoTelegramClient client, telegram4j.tl.ReplyInlineMarkup data) {
        super(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public Type getType() {
        return Type.of(data);
    }

    /**
     * Gets nested lists with {@link KeyboardButton}
     * which contains only {@link KeyboardButton.Type#isInlineOnly(KeyboardButton.Type) inline} buttons.
     *
     * @return The nested with {@link KeyboardButton inline buttons}.
     */
    public List<List<KeyboardButton>> getRows() {
        return data.rows().stream()
                .map(r -> r.buttons().stream()
                        .map(b -> new KeyboardButton(client, b))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplyInlineMarkup that = (ReplyInlineMarkup) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ReplyInlineMarkup{" +
                "data=" + data +
                '}';
    }
}
