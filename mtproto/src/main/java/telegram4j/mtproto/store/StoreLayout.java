package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.tl.*;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.help.UserInfo;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.request.auth.SignIn;
import telegram4j.tl.updates.State;
import telegram4j.tl.users.UserFull;

import java.util.Map;

public interface StoreLayout {

    Mono<State> getCurrentState();

    Mono<Long> getSelfId();

    Mono<ResolvedPeer> resolvePeer(String username);

    Mono<InputUser> resolveUser(long userId);

    Mono<InputChannel> resolveChannel(long channelId);

    Mono<Messages> getMessageById(InputPeer peerId, InputMessage messageId);

    Mono<Chat> getChatMinById(long chatId);

    Mono<ChatFull> getChatFullById(long chatId);

    Mono<User> getUserMinById(long userId);

    Mono<UserFull> getUserFullById(long userId);

    Mono<SignIn> getSignInInfo(String phoneNumber);

    Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc);

    // message updates

    Mono<Void> onNewMessage(Message message, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Message> onEditMessage(Message message, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessagesFields update);

    // user updates

    Mono<Void> onChannelUserTyping(UpdateChannelUserTyping action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onChatUserTyping(UpdateChatUserTyping action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onUserTyping(UpdateUserTyping action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<UserNameFields> onUserNameUpdate(UpdateUserName action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<String> onUserPhoneUpdate(UpdateUserPhone action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<UserProfilePhoto> onUserPhotoUpdate(UpdateUserPhoto action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<UserStatus> onUserStatusUpdate(UpdateUserStatus action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onChatParticipantAdd(UpdateChatParticipantAdd action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onChatParticipantAdmin(UpdateChatParticipantAdmin action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onChatParticipantDelete(UpdateChatParticipantDelete action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onChatParticipant(UpdateChatParticipant action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onChatParticipants(UpdateChatParticipants action, Map<Long, Chat> chats, Map<Long, User> users);

    Mono<Void> onChannelParticipant(UpdateChannelParticipant update, Map<Long, Chat> chats, Map<Long, User> users);

    // not an update-related methods

    Mono<Void> updateSelfId(long userId);

    Mono<Void> updateState(State state);

    Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey);

    Mono<Void> updateSignInInfo(SignIn signInInfo);

    // common request methods

    Mono<User> onUserUpdate(User payload);

    Mono<telegram4j.tl.users.UserFull> onUserUpdate(telegram4j.tl.users.UserFull payload);

    Mono<UserInfo> onUserInfoUpdate(UserInfo payload);

    Mono<Chat> onChatUpdate(Chat payload);

    Mono<telegram4j.tl.messages.ChatFull> onChatUpdate(telegram4j.tl.messages.ChatFull payload);
}
