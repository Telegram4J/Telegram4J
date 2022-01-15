package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;

public final class ReplyKeyboardHide extends ReplyMarkup {

    private final telegram4j.tl.ReplyKeyboardHide data;

    public ReplyKeyboardHide(MTProtoTelegramClient client, telegram4j.tl.ReplyKeyboardHide data) {
        super(client);
        this.data = Objects.requireNonNull(data, "data");
    }

    public boolean isSelective() {
        return data.selective();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplyKeyboardHide that = (ReplyKeyboardHide) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ReplyKeyboardHide{" +
                "data=" + data +
                "} " + super.toString();
    }
}
