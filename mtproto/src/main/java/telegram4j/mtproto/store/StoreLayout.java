package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.tl.*;
import telegram4j.tl.updates.State;

import java.util.List;

public interface StoreLayout {

    Mono<State> getCurrentState();

    Mono<Long> getSelfId();

    Mono<InputPeer> resolvePeer(String username);

    Mono<Message> getMessageById(long chatId, int messageId);

    Mono<Chat> getChatById(long chatId);

    Mono<User> getUserById(long userId);

    Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc);

    Mono<Void> updateSelfId(long userId);

    Mono<Void> updateState(State state);

    Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey);

    Mono<Void> onNewMessage(Message message, List<Chat> chats, List<User> users);

    Mono<Message> onEditMessage(Message message, List<Chat> chats, List<User> users);
}