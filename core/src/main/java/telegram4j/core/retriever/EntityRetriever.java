package telegram4j.core.retriever;

import reactor.core.publisher.Mono;
import telegram4j.core.object.ChatMember;
import telegram4j.core.object.File;
import telegram4j.core.object.Id;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

public interface EntityRetriever {

    Mono<User> getSelf();

    Mono<Chat> getChatById(Id chatId);

    Mono<User> getUserById(Id userId);

    Mono<ChatMember> getChatMemberById(Id chatId, Id userId);

    Mono<File> getFileById(String fileId);
}
