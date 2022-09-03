package telegram4j.core.object.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;

import java.util.Objects;
import java.util.Optional;

public final class ReplyKeyboardForceReply extends ReplyMarkup {

    private final telegram4j.tl.ReplyKeyboardForceReply data;

    public ReplyKeyboardForceReply(MTProtoTelegramClient client, telegram4j.tl.ReplyKeyboardForceReply data) {
        super(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public Type getType() {
        return Type.of(data);
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
                '}';
    }
}
