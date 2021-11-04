package telegram4j.core.object.replymarkup;

import reactor.util.annotation.Nullable;
import telegram4j.json.ReplyMarkupData;

import java.util.Objects;

public class ReplyMarkup {

    private final ReplyMarkupData data;

    public ReplyMarkup(ReplyMarkupData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public ReplyMarkupData getData() {
        return data;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplyMarkup)) return false;
        ReplyMarkup that = (ReplyMarkup) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ReplyMarkup{data=" + data + '}';
    }
}
