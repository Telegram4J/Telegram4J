package telegram4j.core.spec;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.tl.ImmutableInputReplyToMessage;
import telegram4j.tl.InputReplyToMessage;

import java.util.Objects;

public final class ReplyToMessageSpec implements ReplySpec, Spec<ImmutableInputReplyToMessage> {
    private final int messageId;
    @Nullable
    private final Integer topMessageId;

    private ReplyToMessageSpec(int messageId, @Nullable Integer topMessageId) {
        this.messageId = messageId;
        this.topMessageId = topMessageId;
    }

    public static ReplyToMessageSpec of(Message message) {
        return of(message.getId(), null);
    }

    public static ReplyToMessageSpec of(Message message, @Nullable Integer topMessageId) {
        return of(message.getId(), topMessageId);
    }

    public static ReplyToMessageSpec of(int messageId) {
        return of(messageId, null);
    }

    public static ReplyToMessageSpec of(int messageId, @Nullable Integer topMessageId) {
        return new ReplyToMessageSpec(messageId, topMessageId);
    }

    @Override
    public ImmutableInputReplyToMessage resolve() {
        return InputReplyToMessage.builder()
                .replyToMsgId(messageId)
                .topMsgId(topMessageId)
                .build();
    }

    @Override
    public Mono<ImmutableInputReplyToMessage> resolve(MTProtoTelegramClient client) {
        return Mono.just(resolve());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplyToMessageSpec that)) return false;
        return messageId == that.messageId && Objects.equals(topMessageId, that.topMessageId);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + messageId;
        h += (h << 5) + Objects.hashCode(topMessageId);
        return h;
    }

    @Override
    public String toString() {
        return "ReplyToMessageSpec{" +
                "messageId=" + messageId +
                ", topMessageId=" + topMessageId +
                '}';
    }
}
