package telegram4j.core.object;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.MessageReplyStoryHeader;

import java.util.Objects;

public final class MessageReplyToStoryHeader extends MessageReplyHeader {

    private final MessageReplyStoryHeader data;

    public MessageReplyToStoryHeader(MTProtoTelegramClient client, MessageReplyStoryHeader data) {
        super(client);
        this.data = Objects.requireNonNull(data);
    }

    public Id getUserId() {
        return Id.ofUser(data.userId());
    }

    public Mono<User> getUser() {
        return getUser(MappingUtil.IDENTITY_RETRIEVER);
    }

    public Mono<User> getUser(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy)
                .getUserById(getUserId());
    }

    public int getStoryId() {
        return data.storyId();
    }

    @Override
    public String toString() {
        return "MessageReplyToStoryHeader{" +
                "data=" + data +
                '}';
    }
}
