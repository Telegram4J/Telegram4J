package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ReplyInlineMarkup extends ReplyMarkup {

    private final telegram4j.tl.ReplyInlineMarkup data;

    public ReplyInlineMarkup(MTProtoTelegramClient client, telegram4j.tl.ReplyInlineMarkup data) {
        super(client);
        this.data = Objects.requireNonNull(data, "data");
    }

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
                "} " + super.toString();
    }
}
