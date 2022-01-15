package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.util.Objects;
import java.util.Optional;

public final class ReplyKeyboardForceReply extends ReplyMarkup {

    private final telegram4j.tl.ReplyKeyboardForceReply data;

    public ReplyKeyboardForceReply(MTProtoTelegramClient client, telegram4j.tl.ReplyKeyboardForceReply data) {
        super(client);
        this.data = Objects.requireNonNull(data, "data");
    }

    public boolean isSingleUse() {
        return data.singleUse();
    }

    public boolean isSelective() {
        return data.selective();
    }

    public Optional<String> getPlaceholder() {
        return Optional.ofNullable(data.placeholder());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplyKeyboardForceReply that = (ReplyKeyboardForceReply) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ReplyKeyboardForceReply{" +
                "data=" + data +
                "} " + super.toString();
    }
}
