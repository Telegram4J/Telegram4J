package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.contacts.*;
import telegram4j.tl.photos.Photo;
import telegram4j.tl.photos.Photos;
import telegram4j.tl.request.contacts.*;
import telegram4j.tl.request.photos.ImmutableDeletePhotos;
import telegram4j.tl.request.photos.ImmutableGetUserPhotos;
import telegram4j.tl.request.photos.ImmutableUpdateProfilePhoto;
import telegram4j.tl.request.photos.UploadProfilePhoto;
import telegram4j.tl.request.users.ImmutableGetFullUser;
import telegram4j.tl.request.users.ImmutableGetUsers;
import telegram4j.tl.request.users.ImmutableSetSecureValueErrors;
import telegram4j.tl.users.UserFull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UserService extends RpcService {

    public UserService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // additional methods
    // ======================

    /**
     * Retrieve minimal information about user and update cache.
     *
     * @param userId The id of user
     * @return A {@link Mono} emitting on successful completion minimal information about user
     */
    @BotCompatible
    public Mono<User> getUser(InputUser userId) {
        return getUsers(List.of(userId)).mapNotNull(u -> u.isEmpty() ? null : u.get(0));
    }

    // user namespace
    // ======================

    /**
     * Retrieve minimal information about list of users and update cache.
     *
     * @param userIds An iterable of user id elements
     * @return A {@link Mono} emitting list with minimal users
     */
    @BotCompatible
    public Mono<List<User>> getUsers(Iterable<? extends InputUser> userIds) {
        return Mono.defer(() -> client.sendAwait(ImmutableGetUsers.of(userIds)))
                .flatMap(u -> storeLayout.onContacts(List.of(), u)
                        .thenReturn(u));
    }

    /**
     * Retrieve detailed information about user and update cache.
     *
     * @param id The id of the user
     * @return A {@link Mono} emitting on successful completion an object contains
     * detailed info about user and auxiliary data
     */
    @BotCompatible
    public Mono<UserFull> getFullUser(InputUser id) {
        return client.sendAwait(ImmutableGetFullUser.of(id))
                .flatMap(u -> storeLayout.onUserUpdate(u).thenReturn(u));
    }

    public Mono<Boolean> setSecureValueErrors(InputUser id, Iterable<? extends SecureValueError> errors) {
        return Mono.defer(() -> client.sendAwait(ImmutableSetSecureValueErrors.of(id, errors)));
    }

    // contacts namespace
    // ======================

    public Mono<List<Integer>> getContactsIds(long hash) {
        return client.sendAwait(ImmutableGetContactIDs.of(hash));
    }

    public Mono<List<ContactStatus>> getStatuses() {
        return client.sendAwait(GetStatuses.instance());
    }

    public Mono<BaseContacts> getContacts(long hash) {
        return client.sendAwait(ImmutableGetContacts.of(hash))
                .ofType(BaseContacts.class);
    }

    public Mono<ImportedContacts> importContacts(Iterable<? extends InputContact> contacts) {
        return Mono.defer(() -> client.sendAwait(ImmutableImportContacts.of(contacts)));
    }

    public Mono<Updates> deleteContacts(Iterable<? extends InputUser> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableDeleteContacts.of(ids)));
    }

    public Mono<Boolean> deleteByPhones(List<String> phones) {
        return Mono.defer(() -> client.sendAwait(ImmutableDeleteByPhones.of(phones)));
    }

    public Mono<Boolean> block(InputPeer peer) {
        return client.sendAwait(ImmutableBlock.of(peer));
    }

    public Mono<Boolean> unblock(InputPeer peer) {
        return client.sendAwait(ImmutableUnblock.of(peer));
    }

    public Mono<Blocked> getBlocked(int offset, int limit) {
        return client.sendAwait(ImmutableGetBlocked.of(offset, limit));
    }

    public Mono<Found> search(String query, int limit) {
        return client.sendAwait(ImmutableSearch.of(query, limit));
    }

    /**
     * Search peers by substring of query and update cache.
     *
     * @param username The peer full username
     * @return A {@link Mono} emitting on successful completion an object contains
     * info on users found by username and auxiliary data
     */
    @BotCompatible
    public Mono<ResolvedPeer> resolveUsername(String username) {
        return client.sendAwait(ImmutableResolveUsername.of(username))
                .flatMap(d -> storeLayout.onContacts(d.chats(), d.users()).thenReturn(d));
    }

    public Mono<TopPeers> getTopPeers(GetTopPeers request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> resetTopPeerRating(TopPeerCategory category, InputPeer peer) {
        return client.sendAwait(ImmutableResetTopPeerRating.of(category, peer));
    }

    public Mono<Boolean> resetSaves() {
        return client.sendAwait(ResetSaved.instance());
    }

    public Mono<List<SavedContact>> getSaved() {
        return client.sendAwait(GetSaved.instance());
    }

    public Mono<Boolean> toggleTopPeers(boolean enabled) {
        return client.sendAwait(ImmutableToggleTopPeers.of(enabled));
    }

    public Mono<Updates> addContact(AddContact request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> acceptContact(InputUser id) {
        return client.sendAwait(ImmutableAcceptContact.of(id));
    }

    public Mono<Updates> getLocated(GetLocated request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> blockFromReplies(BlockFromReplies request) {
        return client.sendAwait(request);
    }

    public Mono<ResolvedPeer> resolvePhone(String phone) {
        return client.sendAwait(ImmutableResolvePhone.of(phone));
    }

    // photos namespace
    // ======================

    public Mono<Photo> updateProfilePhoto(InputPhoto photo) {
        return client.sendAwait(ImmutableUpdateProfilePhoto.of(photo));
    }

    public Mono<Photo> updateProfilePhoto(String photoFileReferenceId) {
        return Mono.defer(() -> updateProfilePhoto(
                FileReferenceId.deserialize(photoFileReferenceId).asInputPhoto()));
    }

    public Mono<Photo> uploadProfilePhoto(UploadProfilePhoto request) {
        return client.sendAwait(request);
    }

    public Mono<List<Long>> deletePhotos(Iterable<InputPhoto> photos) {
        return Mono.defer(() -> client.sendAwait(ImmutableDeletePhotos.of(photos)));
    }

    public Mono<List<Long>> deletePhotosIds(Iterable<String> photosFileReferenceIds) {
        return Mono.defer(() -> client.sendAwait(ImmutableDeletePhotos.of(
                StreamSupport.stream(photosFileReferenceIds.spliterator(), false)
                        .map(FileReferenceId::deserialize)
                        .map(FileReferenceId::asInputPhoto)
                        .collect(Collectors.toUnmodifiableList()))));
    }

    @BotCompatible
    public Mono<Photos> getUserPhotos(InputUser id, int offset, long maxId, int limit) {
        return client.sendAwait(ImmutableGetUserPhotos.of(id, offset, maxId, limit));
    }
}
