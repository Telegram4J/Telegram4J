package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.tl.*;
import telegram4j.tl.help.UserInfo;
import telegram4j.tl.updates.State;

import java.util.List;

public interface StoreLayout {

    Mono<State> getCurrentState();

    Mono<Long> getSelfId();

    Mono<InputPeer> resolvePeer(String username);

    Mono<Message> getMessageById(long chatId, int messageId);

    Mono<Chat> getChatMinById(long chatId);

    Mono<ChatFull> getChatFullById(long chatId);

    Mono<User> getUserMinById(long userId);

    Mono<UserFull> getUserFullById(long userId);

    Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc);

    // message updates

    Mono<Void> onNewMessage(Message message, List<Chat> chats, List<User> users);

    Mono<Message> onEditMessage(Message message, List<Chat> chats, List<User> users);

    // user updates

    Mono<Void> onChannelUserTyping(UpdateChannelUserTyping action, List<Chat> chats, List<User> users);

    Mono<Void> onChatUserTyping(UpdateChatUserTyping action, List<Chat> chats, List<User> users);

    Mono<Void> onUserTyping(UpdateUserTyping action, List<Chat> chats, List<User> users);

    Mono<UserNameFields> onUserNameUpdate(UpdateUserName action, List<Chat> chats, List<User> users);

    Mono<String> onUserPhoneUpdate(UpdateUserPhone action, List<Chat> chats, List<User> users);

    Mono<UserProfilePhoto> onUserPhotoUpdate(UpdateUserPhoto action, List<Chat> chats, List<User> users);

    Mono<UserStatus> onUserStatusUpdate(UpdateUserStatus action, List<Chat> chats, List<User> users);

    Mono<Void> onChatParticipantAdd(UpdateChatParticipantAdd action, List<Chat> chats, List<User> users);

    Mono<Void> onChatParticipantAdmin(UpdateChatParticipantAdmin action, List<Chat> chats, List<User> users);

    Mono<Void> onChatParticipantDelete(UpdateChatParticipantDelete action, List<Chat> chats, List<User> users);

    Mono<Void> onChatParticipant(UpdateChatParticipant action, List<Chat> chats, List<User> users);

    Mono<Void> onChatParticipants(UpdateChatParticipants action, List<Chat> chats, List<User> users);


    // not an update-related methods

    Mono<Void> updateSelfId(long userId);

    Mono<Void> updateState(State state);

    Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey);

    // common update methods

    Mono<User> onUserUpdate(User payload);

    Mono<telegram4j.tl.users.UserFull> onUserUpdate(telegram4j.tl.users.UserFull payload);

    Mono<UserInfo> onUserInfoUpdate(UserInfo payload);

    Mono<Chat> onChatUpdate(Chat payload);

    Mono<telegram4j.tl.messages.ChatFull> onChatUpdate(telegram4j.tl.messages.ChatFull payload);
}
