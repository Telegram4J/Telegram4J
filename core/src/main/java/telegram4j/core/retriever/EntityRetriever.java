package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import telegram4j.core.object.Id;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

public interface EntityRetriever {

    Mono<PeerEntity> resolveUsername(String username);

    Mono<User> getUserMinById(Id userId);

    Mono<User> getUserFullById(Id userId);

    Mono<Chat> getChatMinById(Id chatId);

    Mono<Chat> getChatFullById(Id chatId);
}
