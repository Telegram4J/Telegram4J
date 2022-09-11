package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.util.Id;
import telegram4j.tl.ChannelForbidden;
import telegram4j.tl.ChatForbidden;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Unchecked exception used to signal unavailable chat/channels for mapping. */
public class UnavailableChatException extends RuntimeException {

    private final Chat.Type type;
    private final Id id;
    private final String title;
    @Nullable
    private final Instant untilTimestamp;

    private UnavailableChatException(Chat.Type type, Id id, String title, @Nullable Instant untilTimestamp) {
        super(String.format(type.name().toLowerCase(Locale.US) + " '%s' with id %s" +
                (untilTimestamp != null ? " until " + untilTimestamp : ""),
                title, id.asString()));
        this.type = type;
        this.id = id;
        this.title = title;
        this.untilTimestamp = untilTimestamp;
    }

    public static UnavailableChatException from(ChannelForbidden data) {
        Chat.Type type = data.megagroup() ? Chat.Type.SUPERGROUP : Chat.Type.CHANNEL;
        Id id = Id.ofChannel(data.id(), data.accessHash());
        Integer untilDate = data.untilDate();
        Instant untilTimestamp = untilDate != null ? Instant.ofEpochSecond(untilDate) : null;
        return new UnavailableChatException(type, id, data.title(), untilTimestamp);
    }

    public static UnavailableChatException from(ChatForbidden data) {
        Id id = Id.ofChat(data.id());
        return new UnavailableChatException(Chat.Type.GROUP, id, data.title(), null);
    }

    public Chat.Type getType() {
        return type;
    }

    public Id getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Optional<Instant> getUntilTimestamp() {
        return Optional.ofNullable(untilTimestamp);
    }
}
