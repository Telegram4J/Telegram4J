package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.tl.InputMessage;

import java.util.Objects;

/** Additional wrapping for {@code EntityRetriever} which delegates methods to 2 {@link EntityRetriever}s. */
public class FallbackEntityRetriever implements EntityRetriever {
    private final EntityRetriever first;
    private final EntityRetriever second;

    FallbackEntityRetriever(EntityRetriever first, EntityRetriever second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        return first.resolvePeer(peerId)
                .switchIfEmpty(second.resolvePeer(peerId));
    }

    @Override
    public Mono<User> getUserById(Id userId) {
        return first.getUserById(userId)
                .switchIfEmpty(second.getUserById(userId));
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        return first.getUserMinById(userId)
                .switchIfEmpty(second.getUserMinById(userId));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        return first.getUserFullById(userId)
                .switchIfEmpty(second.getUserFullById(userId));
    }

    @Override
    public Mono<Chat> getChatById(Id chatId) {
        return first.getChatById(chatId)
                .switchIfEmpty(second.getChatById(chatId));
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return first.getChatMinById(chatId)
                .switchIfEmpty(second.getChatMinById(chatId));
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return first.getChatFullById(chatId)
                .switchIfEmpty(second.getChatFullById(chatId));
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Iterable<? extends InputMessage> messageIds) {
        return first.getMessagesById(messageIds)
                .switchIfEmpty(second.getMessagesById(messageIds));
    }

    @Override
    public Mono<AuxiliaryMessages> getMessagesById(Id channelId, Iterable<? extends InputMessage> messageIds) {
        return first.getMessagesById(channelId, messageIds)
                .switchIfEmpty(second.getMessagesById(channelId, messageIds));
    }
}
