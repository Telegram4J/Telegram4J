package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.tl.*;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.help.UserInfo;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.updates.State;
import telegram4j.tl.users.UserFull;

import java.util.Map;

/**
 * Storage interface for interacting with tl entities and session information.
 * Due to message ids specific differ from user to user,
 * the storage cannot be used by more than one user.
 */
public interface StoreLayout {

    /**
     * Retrieve local updates state.
     *
     * @return A {@link Mono} emitting on successful completion the {@link State}
     * object with info about local updates state.
     */
    Mono<State> getCurrentState();

    /**
     * Retrieve self user id.
     *
     * @return A {@link Mono} emitting on successful completion a self user id.
     */
    Mono<Long> getSelfId();

    /**
     * Search channel or user by the specified username
     * and compute container with minimal found info.
     *
     * @implSpec the implementation must support <b>me</b> and <b>self</b>
     * aliases to retrieve information about itself.
     *
     * @param username The username of channel or user.
     * @return A {@link Mono} emitting on successful completion
     * a container with minimal information about found peer.
     */
    Mono<ResolvedPeer> resolvePeer(String username);

    /**
     * Resolve input user id from specified user id.
     * This method can be used to get access hash
     * or to check if id equals self id.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion
     * the {@link BaseInputUser} with access hash
     * or {@link InputUserFromMessage} if user is min
     * and the storage isn't associated with bot.
     */
    Mono<InputUser> resolveUser(long userId);

    /**
     * Resolve input channel id from specified channel id.
     * This method can be used to get access hash.
     *
     * @param channelId The id of channel.
     * @return A {@link Mono} emitting on successful completion
     * the {@link BaseInputChannel} with access hash
     * or {@link InputChannelFromMessage} if channel is min
     * and the storage isn't associated with bot.
     */
    Mono<InputChannel> resolveChannel(long channelId);

    /**
     * Check existence of message.
     * Currently used only in updates handling.
     *
     * @param message The ordinal or service message to check.
     * @return A {@link Mono} emitting on successful completion {@code true} if message exists.
     */
    Mono<Boolean> existMessage(BaseMessageFields message);

    /**
     * Retrieve user/group chat's messages with auxiliary data by given ids.
     * Ids with type {@link InputMessagePinned} will be ignored.
     *
     * @param messageIds An iterable of message id elements.
     * @return A {@link Mono} emitting on successful completion
     * the container with found messages and auxiliary data.
     */
    Mono<Messages> getMessages(Iterable<? extends InputMessage> messageIds);

    /**
     * Retrieve channel's messages with auxiliary data by given ids.
     *
     * @param channelId The id of channel.
     * @param messageIds An iterable of message id elements.
     * @return A {@link Mono} emitting on successful completion
     * the container with found messages and auxiliary data.
     */
    Mono<Messages> getMessages(long channelId, Iterable<? extends InputMessage> messageIds);

    /**
     * Retrieve minimal chat/channel information by specified id.
     *
     * @param chatId The id of chat/channel.
     * @return A {@link Mono} emitting on successful completion
     * the {@link BaseChat} or {@link Channel} object.
     */
    Mono<Chat> getChatMinById(long chatId);

    /**
     * Retrieve detailed chat/channel information by specified id.
     *
     * @param chatId The id of chat/channel.
     * @return A {@link Mono} emitting on successful completion
     * the {@link ChatFull} container with detailed and minimal information about chat/channel.
     */
    Mono<ChatFull> getChatFullById(long chatId);

    /**
     * Retrieve minimal user information by specified id.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion the {@link BaseUser} object.
     */
    Mono<User> getUserMinById(long userId);

    /**
     * Retrieve detailed user information by specified id.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion
     * the {@link UserFull} container with detailed and minimal information about user.
     */
    Mono<UserFull> getUserFullById(long userId);

    /**
     * Retrieve auth key holder, associated with specified dc.
     *
     * @param dc The id of datacenter.
     * @return A {@link Mono} emitting on successful completion
     * the {@link AuthorizationKeyHolder} object with auth key and id.
     */
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

    // common request methods

    Mono<User> onUserUpdate(User payload);

    Mono<telegram4j.tl.users.UserFull> onUserUpdate(telegram4j.tl.users.UserFull payload);

    Mono<UserInfo> onUserInfoUpdate(UserInfo payload);

    Mono<Chat> onChatUpdate(Chat payload);

    Mono<telegram4j.tl.messages.ChatFull> onChatUpdate(telegram4j.tl.messages.ChatFull payload);

    Mono<ResolvedPeer> onResolvedPeer(ResolvedPeer payload);
}
