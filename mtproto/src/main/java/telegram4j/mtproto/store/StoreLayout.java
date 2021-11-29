package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.tl.*;

import java.util.List;

public interface StoreLayout {

    Mono<Long> getSelfId();

    Mono<InputPeer> resolvePeer(String username);

    Mono<Message> getMessageById(long chatId, int messageId);

    Mono<Chat> getChatById(long chatId);

    Mono<User> getUserById(long userId);

    Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc);

    Mono<Void> updateSelfId(long userId);

    Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey);

    Mono<Void> onNewMessage(UpdateNewMessage update, List<Chat> chats, List<User> users);

    Mono<Message> onEditMessage(UpdateEditMessage update, List<Chat> chats, List<User> users);
}
