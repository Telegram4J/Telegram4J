package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.channels.AdminLogResults;
import telegram4j.tl.channels.ChannelParticipant;
import telegram4j.tl.channels.ChannelParticipants;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.*;
import telegram4j.tl.request.channels.DeleteMessages;
import telegram4j.tl.request.channels.GetMessages;
import telegram4j.tl.request.channels.ImmutableDeleteHistory;
import telegram4j.tl.request.channels.ImmutableReadHistory;
import telegram4j.tl.request.channels.ReadMessageContents;
import telegram4j.tl.request.channels.ReportSpam;
import telegram4j.tl.request.channels.*;
import telegram4j.tl.request.folders.EditPeerFolders;
import telegram4j.tl.request.folders.ImmutableDeleteFolder;
import telegram4j.tl.request.messages.*;

import java.util.List;

/** Rpc service with chat and channel related methods. */
public class ChatService extends RpcService {

    public ChatService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    /**
     * Retrieve minimal chat by given id.
     *
     * @param id The id of chat
     * @return A {@link Mono} emitting on successful completion minimal information about chat
     */
    public Mono<Chat> getChat(long id) {
        return getChats(List.of(id))
                .ofType(BaseChats.class)
                .map(c -> c.chats().get(0));
    }

    /**
     * Retrieve minimal chats by their ids.
     *
     * @param ids An iterable of chat id elements
     * @return A {@link Mono} emitting on successful completion a list of
     * minimal chats or slice of list if there are a lot of chats
     */
    public Mono<Chats> getChats(Iterable<Long> ids) {
        return client.sendAwait(GetChats.builder().id(ids).build());
    }

    /**
     * Retrieve detailed information about chat by their id.
     *
     * @param chatId The id of the chat.
     * @return A {@link Mono} emitting on successful completion an object contains
     * detailed info about chat and auxiliary data
     */
    public Mono<ChatFull> getFullChat(long chatId) {
        return client.sendAwait(ImmutableGetFullChat.of(chatId))
                .flatMap(c -> storeLayout.onChatUpdate(c).thenReturn(c));
    }

    public Mono<Chat> editChatTitle(long chatId, String title) {
        return client.sendAwait(ImmutableEditChatTitle.of(chatId, title))
                .flatMap(u -> {
                    switch (u.identifier()) {
                        case BaseUpdates.ID:
                            BaseUpdates casted = (BaseUpdates) u;

                            client.updates().emitNext(u, Sinks.EmitFailureHandler.FAIL_FAST);

                            return Mono.justOrEmpty(casted.chats().stream()
                                    .filter(c -> c.id() == chatId)
                                    .findFirst());
                        default:
                            return Mono.error(new IllegalArgumentException("Unknown updates type: " + u));
                    }
                });
    }

    public Mono<Updates> editChatPhoto(long chatId, InputChatPhoto photo) {
        return client.sendAwait(ImmutableEditChatPhoto.of(chatId, photo));
    }

    public Mono<Updates> addChatUser(long chatId, InputUser user, int fwdLimit) {
        return client.sendAwait(ImmutableAddChatUser.of(chatId, user, fwdLimit));
    }

    public Mono<Updates> deleteChatUser(DeleteChatUser request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> createChat(Iterable<? extends InputUser> users, String title) {
        return client.sendAwait(CreateChat.builder()
                .users(users)
                .title(title)
                .build());
    }

    // Folder methods

    public Mono<Updates> editPeerFolders(Iterable<? extends InputFolderPeer> peers) {
        return client.sendAwait(EditPeerFolders.builder()
                .folderPeers(peers)
                .build());
    }

    public Mono<Updates> deleteFolder(int folderId) {
        return client.sendAwait(ImmutableDeleteFolder.of(folderId));
    }

    // Channel related methods

    public Mono<Boolean> readHistory(InputChannel channel, int maxId) {
        return client.sendAwait(ImmutableReadHistory.of(channel, maxId));
    }

    public Mono<AffectedMessages> deleteMessages(InputChannel channel, Iterable<Integer> ids) {
        return client.sendAwait(DeleteMessages.builder()
                .channel(channel)
                .id(ids)
                .build());
    }

    public Mono<Boolean> reportSpam(InputChannel channel, InputPeer participant, Iterable<Integer> ids) {
        return client.sendAwait(ReportSpam.builder()
                .channel(channel)
                .participant(participant)
                .id(ids)
                .build());
    }

    public Mono<Messages> getMessages(InputChannel channel, Iterable<? extends InputMessage> ids) {
        return client.sendAwait(GetMessages.builder()
                .channel(channel)
                .id(ids)
                .build());
    }

    public Mono<ChannelParticipants> getParticipants(InputChannel channel, ChannelParticipantsFilter filter,
                                                     int offset, int limit, Iterable<Long> ids) {
        return getParticipants(channel, filter, offset, limit, calculatePaginationHash(ids));
    }

    public Mono<ChannelParticipants> getParticipants(InputChannel channel, ChannelParticipantsFilter filter,
                                                     int offset, int limit, long hash) {
        return client.sendAwait(ImmutableGetParticipants.of(channel, filter, offset, limit, hash));
    }

    public Mono<ChannelParticipant> getParticipant(InputChannel channel, InputPeer peer) {
        return client.sendAwait(ImmutableGetParticipant.of(channel, peer));
    }

    /**
     * Retrieve minimal channel by given id.
     *
     * @param id The id of channel
     * @return A {@link Mono} emitting on successful completion minimal information about channel
     */
    public Mono<Chat> getChannel(InputChannel id) {
        return getChannels(List.of(id))
                .ofType(BaseChats.class)
                .map(chats -> chats.chats().get(0));
    }

    /**
     * Retrieve minimal channels by their ids.
     *
     * @param ids An iterable of channel id elements
     * @return A {@link Mono} emitting on successful completion a list of
     * minimal channels or slice of list if there are a lot of channels
     */
    public Mono<Chats> getChannels(Iterable<? extends InputChannel> ids) {
        return client.sendAwait(GetChannels.builder().id(ids).build());
    }

    /**
     * Retrieve detailed channel by given id.
     *
     * @param id The id of channel
     * @return A {@link Mono} emitting on successful completion detailed information about channel
     */
    public Mono<ChatFull> getFullChannel(InputChannel id) {
        return client.sendAwait(ImmutableGetFullChannel.of(id));
    }

    // TODO: check updates type
    public Mono<Updates> createChannel(CreateChannel request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> editAdmin(InputChannel channel, InputUser user, ChatAdminRights rights, String rank) {
        return client.sendAwait(ImmutableEditAdmin.of(channel, user, rights, rank));
    }

    public Mono<Updates> editTitle(InputChannel channel, String title) {
        return client.sendAwait(ImmutableEditTitle.of(channel, title));
    }

    public Mono<Updates> editPhoto(InputChannel channel, InputChatPhoto photo) {
        return client.sendAwait(ImmutableEditPhoto.of(channel, photo));
    }

    /**
     * Check if a username is free and can be assigned to a channel/supergroup
     * @apiNote This method not available for bots.
     *
     * @param channel the channel/supergroup that will assign the specified username
     * @param username the username to check
     * @return A {@link Mono} emitting on successful completion {@code true}
     */
    public Mono<Boolean> checkUsername(InputChannel channel, String username) {
        return client.sendAwait(ImmutableCheckUsername.of(channel, username));
    }

    /**
     * Change the username of a supergroup/channel.
     * @apiNote This method not available for bots.
     *
     * @param channel the channel/supergroup that will assign the specified username
     * @param username the username to update
     * @return A {@link Mono} emitting on successful completion {@code true}
     */
    public Mono<Boolean> updateUsername(InputChannel channel, String username) {
        return client.sendAwait(ImmutableUpdateUsername.of(channel, username));
    }

    public Mono<Updates> joinChannel(InputChannel channel) {
        return client.sendAwait(ImmutableJoinChannel.of(channel));
    }

    public Mono<Updates> leaveChannel(InputChannel channel) {
        return client.sendAwait(ImmutableLeaveChannel.of(channel));
    }

    public Mono<Updates> inviteToChannel(InputChannel channel, Iterable<? extends InputUser> ids) {
        return client.sendAwait(InviteToChannel.builder()
                .channel(channel)
                .users(ids)
                .build());
    }

    public Mono<Updates> deleteChannel(InputChannel channel) {
        return client.sendAwait(ImmutableDeleteChannel.of(channel));
    }

    public Mono<ExportedMessageLink> exportMessageLink(ExportMessageLink request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> toggleSignatures(InputChannel channel, boolean enabled) {
        return client.sendAwait(ImmutableToggleSignatures.of(channel, enabled));
    }

    public Mono<Chats> getAdminedPublicChannels(GetAdminedPublicChannels request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> editBanned(InputChannel channel, InputPeer participant, ChatBannedRights rights) {
        return client.sendAwait(ImmutableEditBanned.of(channel, participant, rights));
    }

    public Mono<AdminLogResults> getAdminLog(InputChannel channel, String query, @Nullable ChannelAdminLogEventsFilter filter,
                                             @Nullable Iterable<? extends InputUser> admins, long maxId, long minId,
                                             int limit) {
        return client.sendAwait(GetAdminLog.builder()
                .channel(channel)
                .q(query)
                .eventsFilter(filter)
                .admins(admins)
                .maxId(maxId)
                .minId(minId)
                .limit(limit)
                .build());
    }

    public Mono<Boolean> setStickers(InputChannel channel, InputStickerSet stickerSet) {
        return client.sendAwait(ImmutableSetStickers.of(channel, stickerSet));
    }

    public Mono<Boolean> readMessageContents(InputChannel channel, Iterable<Integer> ids) {
        return client.sendAwait(ReadMessageContents.builder()
                .channel(channel)
                .id(ids)
                .build());
    }

    public Mono<Boolean> deleteHistory(InputChannel channel, int maxId) {
        return client.sendAwait(ImmutableDeleteHistory.of(channel, maxId));
    }

    public Mono<Updates> togglePreHistoryHidden(InputChannel channel, boolean enabled) {
        return client.sendAwait(ImmutableTogglePreHistoryHidden.of(channel, enabled));
    }

    public Mono<Chats> getLeftChannels(int offset) {
        return client.sendAwait(ImmutableGetLeftChannels.of(offset));
    }

    public Mono<Chats> getGroupsForDiscussion() {
        return client.sendAwait(GetGroupsForDiscussion.instance());
    }

    public Mono<Boolean> setDiscussionGroups(InputChannel broadcast, InputChannel group) {
        return client.sendAwait(ImmutableSetDiscussionGroup.of(broadcast, group));
    }

    public Mono<Updates> editCreator(InputChannel channel, InputUser user, InputCheckPasswordSRP password) {
        return client.sendAwait(ImmutableEditCreator.of(channel, user, password));
    }

    public Mono<Boolean> editLocation(InputChannel channel, InputGeoPoint geoPint, String address) {
        return client.sendAwait(ImmutableEditLocation.of(channel, geoPint, address));
    }

    public Mono<Updates> toggleSlowMode(InputChannel channel, int seconds) {
        return client.sendAwait(ImmutableToggleSlowMode.of(channel, seconds));
    }

    public Mono<InactiveChats> getInactiveChats() {
        return client.sendAwait(GetInactiveChannels.instance());
    }

    public Mono<Updates> convertToGigagroup(InputChannel channel) {
        return client.sendAwait(ImmutableConvertToGigagroup.of(channel));
    }

    public Mono<Boolean> viewSponsoredMessages(InputChannel channel, byte[] randomId) {
        return client.sendAwait(ImmutableViewSponsoredMessage.of(channel, randomId));
    }

    public Mono<SponsoredMessages> getSponsoredMessages(InputChannel channel) {
        return client.sendAwait(ImmutableGetSponsoredMessages.of(channel));
    }
}
