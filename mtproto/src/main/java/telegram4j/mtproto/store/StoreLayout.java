package telegram4j.mtproto.store;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcOptions;
import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.store.object.*;
import telegram4j.tl.*;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.channels.BaseChannelParticipants;
import telegram4j.tl.channels.ChannelParticipant;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.updates.State;
import telegram4j.tl.users.UserFull;

/**
 * Storage interface for interacting with tl entities and session information.
 *
 * @implSpec The implementation must be thread-safe and associated to one dc and user,
 * because of relativity of the message ids from user to user.
 */
public interface StoreLayout {

    Mono<Void> initialize();

    // region retrieve methods

    /**
     * Retrieve main dc to which store assigned.
     *
     * @return A {@link Mono} emitting on successful completion the {@link DataCenter dc} id
     */
    Mono<DataCenter> getDataCenter();

    /**
     * Retrieve local updates state.
     *
     * @return A {@link Mono} emitting on successful completion the {@link State}
     * object with info about local updates state.
     */
    Mono<State> getCurrentState();

    Mono<DcOptions> getDcOptions();

    Mono<PublicRsaKeyRegister> getPublicRsaKeyRegister();

    /**
     * Retrieve auth key holder, associated with specified dc.
     *
     * @param dc The id of datacenter.
     * @return A {@link Mono} emitting on successful completion
     * the {@link AuthorizationKeyHolder} object with auth key and id.
     */
    Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc);

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
     * @implSpec The implementation must support <b>me</b> and <b>self</b>
     * aliases to retrieve information about itself.
     *
     * @param username The username of channel or user.
     * @return A {@link Mono} emitting on successful completion
     * a container with minimal information about found peer.
     */
    Mono<ResolvedPeer> resolvePeer(String username);

    /**
     * Resolve input peer from specified peer id.
     * This method can be used to get access hash
     * or to check if id equals self id.
     *
     * @param peerId The id of peer.
     * @return A {@link Mono} emitting on successful completion
     * the {@link InputPeer} object, but not {@link InputPeerEmpty}.
     */
    Mono<InputPeer> resolvePeer(Peer peerId);

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
     * <p>Currently used only in updates handling.
     *
     * @param message The ordinal or service message to check.
     * @return A {@link Mono} emitting on successful completion {@code true} if message exists.
     */
    Mono<Boolean> existMessage(BaseMessageFields message);

    /**
     * Retrieve user/group chat's messages with auxiliary data by given ids.
     *
     * @implNote Implementation can emit {@link UnsupportedOperationException} for types of the {@code InputMessage}
     * which can't be handled.
     *
     * @param messageIds An iterable of message id elements.
     * @return A {@link Mono} emitting on successful completion
     * the container with found messages and auxiliary data.
     */
    Mono<Messages> getMessages(Iterable<? extends InputMessage> messageIds);

    /**
     * Retrieve channel's messages with auxiliary data by given ids.
     *
     * @implNote Implementation can emit {@link UnsupportedOperationException} for types of the {@code InputMessage}
     * which can't be handled.
     *
     * @param channelId The id of channel.
     * @param messageIds An iterable of message id elements.
     * @return A {@link Mono} emitting on successful completion
     * the container with found messages and auxiliary data.
     */
    Mono<Messages> getMessages(long channelId, Iterable<? extends InputMessage> messageIds);

    /**
     * Retrieve minimal chat information by specified id.
     *
     * @param chatId The id of chat.
     * @return A {@link Mono} emitting on successful completion
     * the {@link BaseChat} object.
     */
    Mono<BaseChat> getChatMinById(long chatId);

    /**
     * Retrieve detailed chat information by specified id.
     *
     * @param chatId The id of chat.
     * @return A {@link Mono} emitting on successful completion
     * the {@link ChatFull} container with detailed and minimal information about chat.
     */
    Mono<ChatFull> getChatFullById(long chatId);

    Mono<ChatData<BaseChat, BaseChatFull>> getChatById(long chatId);

    /**
     * Retrieve minimal channel information by specified id.
     *
     * @param channelId The id of chat.
     * @return A {@link Mono} emitting on successful completion
     * the {@link Channel} object.
     */
    Mono<Channel> getChannelMinById(long channelId);

    /**
     * Retrieve detailed channel information by specified id.
     *
     * @param channelId The id of channel.
     * @return A {@link Mono} emitting on successful completion
     * the {@link ChatFull} container with detailed and minimal information about channel.
     */
    Mono<ChatFull> getChannelFullById(long channelId);

    Mono<ChatData<Channel, telegram4j.tl.ChannelFull>> getChannelById(long channelId);

    /**
     * Retrieve minimal user information by specified id.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion the {@link BaseUser} object.
     */
    Mono<BaseUser> getUserMinById(long userId);

    /**
     * Retrieve detailed user information by specified id.
     *
     * @param userId The id of user.
     * @return A {@link Mono} emitting on successful completion
     * the {@link UserFull} container with detailed and minimal information about user.
     */
    Mono<UserFull> getUserFullById(long userId);

    Mono<PeerData<BaseUser, telegram4j.tl.UserFull>> getUserById(long userId);

    /**
     * Retrieve channel participant by specified channel id and peer id.
     *
     * @param channelId The id of channel.
     * @param peerId The id of participant.
     * @return A {@link Mono} emitting on successful completion
     * the {@link ChannelParticipant} container with channel participant and peer info.
     */
    Mono<ChannelParticipant> getChannelParticipantById(long channelId, Peer peerId);

    /**
     * Retrieve channel participants by specified channel id.
     *
     * @param channelId The id of channel.
     * @return A {@link Mono} emitting on successful completion
     * the {@link ChannelParticipant} containers with channel participant and peer info.
     */
    Flux<ChannelParticipant> getChannelParticipants(long channelId);

    /**
     * Retrieve group chat participant by specified chat id and user id.
     *
     * @param chatId The id of group chat.
     * @param userId The id of participant.
     * @return A {@link Mono} emitting on successful completion
     * the {@link ResolvedChatParticipant participant} with optional user info.
     */
    Mono<ResolvedChatParticipant> getChatParticipantById(long chatId, long userId);

    /**
     * Retrieve group chat participants by specified chat id.
     *
     * @param chatId The id of group chat.
     * @return A {@link Flux} which continually emits
     * the {@link ResolvedChatParticipant} containers with chat participant and their user info.
     */
    Flux<ResolvedChatParticipant> getChatParticipants(long chatId);

    /**
     * Retrieve message poll by specified id.
     *
     * @param pollId The {@link Poll#id() id} of poll.
     * @return A {@link Mono} emitting on successful completion
     * the original {@link MessagePoll poll}.
     */
    Mono<MessagePoll> getPollById(long pollId);

    // endregion
    // region updates

    Mono<Void> onNewMessage(Message update);

    Mono<Message> onEditMessage(Message update);

    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessagesFields update);

    Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessagesFields payload);

    Mono<Void> onChatParticipant(UpdateChatParticipant payload);

    Mono<Void> onChannelParticipant(UpdateChannelParticipant payload);

    Mono<Void> onChatParticipants(ChatParticipants payload);

    // endregion
    // region state methods

    /**
     * Initializes the local main dc id of the store according to the given {@link DataCenter} id.
     *
     * @param dc The main dc id.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> updateDataCenter(DataCenter dc);

    /**
     * Updates the local updates state of the store according to the given {@link State} object.
     *
     * @param state The new updates state.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> updateState(State state);

    Mono<Void> updateDcOptions(DcOptions dcOptions);

    Mono<Void> updatePublicRsaKeyRegister(PublicRsaKeyRegister publicRsaKeyRegister);

    /**
     * Updates the local auth key state of the store according to the given {@link AuthorizationKeyHolder auth key}.
     *
     * @param dc The id of dc that the auth key is associated with.
     * @param authKey The new auth key.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> updateAuthorizationKey(DataCenter dc, AuthorizationKeyHolder authKey);

    /**
     * Updates the local {@link ChannelFull#pts() channel pts} state of the store according to the given pts.
     *
     * @param channelId The id of channel.
     * @param pts The new pts state.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> updateChannelPts(long channelId, int pts);

    Mono<Void> registerPoll(Peer peerId, int messageId, InputMediaPoll poll);

    // endregion
    // region requests hooks

    /**
     * Applies given peer entities to local store.
     *
     * @param chats An iterable with chats.
     * @param users An iterable with users.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users);

    /**
     * Applies given full user to local store.
     *
     * @param payload The user full payload.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onUserUpdate(telegram4j.tl.users.UserFull payload);

    /**
     * Applies given full chat to local store.
     *
     * @param payload The user chat payload.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onChatUpdate(telegram4j.tl.messages.ChatFull payload);

    /**
     * Applies given channel participants list to local store.
     *
     * @param channelId The id of channel.
     * @param payload The channel participants list.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onChannelParticipants(long channelId, BaseChannelParticipants payload);

    /**
     * Applies given channel participant to local store.
     *
     * @param channelId The id of channel.
     * @param payload The channel participant.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onChannelParticipant(long channelId, ChannelParticipant payload);

    /**
     * Applies given messages list to local store.
     *
     * @param payload The messages list.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onMessages(Messages payload);

    Mono<Void> onAuthorization(BaseAuthorization auth);

    // endregion
}
