package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReplyKeyboardMarkup extends ReplyMarkup {

    private final telegram4j.tl.ReplyKeyboardMarkup data;

    public ReplyKeyboardMarkup(MTProtoTelegramClient client, telegram4j.tl.ReplyKeyboardMarkup data) {
        super(client);
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public Type getType() {
        return Type.of(data);
    }

    public boolean isResize() {
        return data.resize();
    }

    public boolean isSingleUse() {
        return data.singleUse();
    }

    public boolean isSelective() {
        return data.selective();
    }

    public List<List<KeyboardButton>> getRows() {
        return data.rows().stream()
                .map(r -> r.buttons().stream()
                        .map(b -> new KeyboardButton(client, b))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

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
