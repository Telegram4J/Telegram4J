package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import telegram4j.core.object.Id;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.PeerId;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

/** Interface to accessing telegram entities. */
public interface EntityRetriever {

    /**
     * Search peer entity by the specified {@link PeerId id}.
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
     * Retrieve chat with minimal information by the specified id.
     *
     * @param chatId The id of chat.
     * @return A {@link Mono} emitting on successful completion
     * the {@link Chat} with minimal information.
     */
    Mono<Chat> getChatMinById(Id chatId);

    /**
     * Retrieve chat with detailed information by the specified id.
     *
     * @param chatId The id of chat.
     * @return A {@link Mono} emitting on successful completion
     * the {@link Chat} with detailed information.
     */
    Mono<Chat> getChatFullById(Id chatId);
}
