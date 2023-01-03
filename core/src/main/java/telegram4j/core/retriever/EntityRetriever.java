package telegram4j.core.retriever;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.tl.InputMessage;

/** Interface to accessing telegram entities. */
public interface EntityRetriever {

    /**
     * Search peer entity by the specified {@link PeerId id}.
     * This method will not return a {@link PrivateChat}.
     *
     * @implSpec the implementation must support <b>me</b> and <b>self</b>
     * aliases to retrieve information about itself.
     *
     * @param peerId The {@link PeerId id} of entity.
     * @return A {@link Mono} emitting on successful completion
     * the {@link PeerEntity} object with specified {@link PeerId}.
     */
    Mono<PeerEntity> resolvePeer(PeerId peerId);

    /**
     * Retrieve user with retriever strategy by the specified id.
     * By default, this method will retrieve minimal information about user,
     * but implementation may replace it with more optimal variant.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion the {@link User}.
     */
    default Mono<User> getUserById(Id userId) {
        return getUserMinById(userId);
    }

    /**
     * Retrieve user with minimal information by the specified id.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion
     * the {@link User} with minimal information.
     */
    Mono<User> getUserMinById(Id userId);

    /**
     * Retrieve user with detailed information by the specified id.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion
     * the {@link User} with detailed information.
     */
    Mono<User> getUserFullById(Id userId);

    /**
     * Retrieve chat with retriever strategy by the specified id.
     * By default, this method will retrieve minimal information about chat,
     * but implementation may replace it with more optimal variant.
     * For {@link PrivateChat} implementation may provide optional information about self user.
     *
     * @param chatId The id of chat.
     * @return A {@link Mono} emitting on successful completion the {@link Chat}.
     */
    default Mono<Chat> getChatById(Id chatId) {
        return getChatMinById(chatId);
    }

    /**
     * Retrieve chat with minimal information by the specified id.
     *
     * @implNote The implementation may retrieve information about self user for {@link PrivateChat}.
     *
     * @param chatId The id of chat.
     * @return A {@link Mono} emitting on successful completion
     * the {@link Chat} with minimal information.
     */
    Mono<Chat> getChatMinById(Id chatId);

    /**
     * Retrieve chat with detailed information by the specified id.
     *
     * @implNote The implementation may retrieve information about self user for {@link PrivateChat}.
     *
     * @param chatId The id of chat.
     * @return A {@link Mono} emitting on successful completion
     * the {@link Chat} with detailed information.
     */
    Mono<Chat> getChatFullById(Id chatId);

    /**
     * Retrieve chat participant with optional peer information by the specified chat id and peer id.
     *
     * @param chatId The id of chat.
     * @param peerId The id of peer.
     * @return A {@link Mono} emitting on successful completion
     * the {@link ChatParticipant} with optional peer information.
     */
    Mono<ChatParticipant> getParticipantById(Id chatId, Id peerId);

    /**
     * Retrieve chat participants with optional peer information by the specified chat id.
     *
     * @param chatId The id of chat.
     * @return A {@link Flux} which continually emits
     * the {@link ChatParticipant} with optional peer information.
     */
    Flux<ChatParticipant> getParticipants(Id chatId);

    /**
     * Retrieve messages from channel or chat with auxiliary data by the specified chat id and message ids.
     * <p> Not all types of {@code InputMessage} can be processed, for example {@code InputMessagePinned} can't be
     * used for user/group chats.
     *
     * @implSpec Auxiliary data must contain chat and authors of message if available.
     *
     * @param chatId The id of chat, can be optional for messages from DMs and group chats.
     * @param messageIds An iterable of message id elements.
     * @return A {@link Mono} emitting on successful completion
     * the {@link AuxiliaryMessages} with resolved messages and auxiliary data.
     */
    Mono<AuxiliaryMessages> getMessages(@Nullable Id chatId, Iterable<? extends InputMessage> messageIds);
}
