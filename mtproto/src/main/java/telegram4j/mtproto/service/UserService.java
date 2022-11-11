package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.service.Compatible.Type;
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

public class UserService extends RpcService {

    public UserService(MTProtoClientGroup groupManager, StoreLayout storeLayout) {
        super(groupManager, storeLayout);
    }

    // additional methods
    // ======================

    /**
     * Retrieve minimal information about user and update cache.
     *
     * @param userId The id of user
     * @return A {@link Mono} emitting on successful completion minimal information about user
     */
    @Compatible(Type.BOTH)
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
    @Compatible(Type.BOTH)
    public Mono<List<User>> getUsers(Iterable<? extends InputUser> userIds) {
        return Mono.defer(() -> sendMain(ImmutableGetUsers.of(userIds)))
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
    @Compatible(Type.BOTH)
    public Mono<UserFull> getFullUser(InputUser id) {
        return sendMain(ImmutableGetFullUser.of(id))
                .flatMap(u -> storeLayout.onUserUpdate(u).thenReturn(u));
    }

    public Mono<Boolean> setSecureValueErrors(InputUser id, Iterable<? extends SecureValueError> errors) {
        return Mono.defer(() -> sendMain(ImmutableSetSecureValueErrors.of(id, errors)));
    }

    // contacts namespace
    // ======================

    public Mono<List<Integer>> getContactsIds(long hash) {
        return sendMain(ImmutableGetContactIDs.of(hash));
    }

    public Mono<List<ContactStatus>> getStatuses() {
        return sendMain(GetStatuses.instance());
    }

    public Mono<BaseContacts> getContacts(long hash) {
        return sendMain(ImmutableGetContacts.of(hash))
                .ofType(BaseContacts.class);
    }

    public Mono<ImportedContacts> importContacts(Iterable<? extends InputContact> contacts) {
        return Mono.defer(() -> sendMain(ImmutableImportContacts.of(contacts)));
    }

    public Mono<Updates> deleteContacts(Iterable<? extends InputUser> ids) {
        return Mono.defer(() -> sendMain(ImmutableDeleteContacts.of(ids)));
    }

    public Mono<Boolean> deleteByPhones(List<String> phones) {
        return Mono.defer(() -> sendMain(ImmutableDeleteByPhones.of(phones)));
    }

    public Mono<Boolean> block(InputPeer peer) {
        return sendMain(ImmutableBlock.of(peer));
    }

    public Mono<Boolean> unblock(InputPeer peer) {
        return sendMain(ImmutableUnblock.of(peer));
    }

    public Mono<Blocked> getBlocked(int offset, int limit) {
        return sendMain(ImmutableGetBlocked.of(offset, limit));
    }

    public Mono<Found> search(String query, int limit) {
        return sendMain(ImmutableSearch.of(query, limit));
    }

    /**
     * Search peers by substring of query and update cache.
     *
     * @param username The peer full username
     * @return A {@link Mono} emitting on successful completion an object contains
     * info on users found by username and auxiliary data
     */
    @Compatible(Type.BOTH)
    public Mono<ResolvedPeer> resolveUsername(String username) {
        return sendMain(ImmutableResolveUsername.of(username))
                .flatMap(d -> storeLayout.onContacts(d.chats(), d.users()).thenReturn(d));
    }

    public Mono<TopPeers> getTopPeers(GetTopPeers request) {
        return sendMain(request);
    }

    public Mono<Boolean> resetTopPeerRating(TopPeerCategory category, InputPeer peer) {
        return sendMain(ImmutableResetTopPeerRating.of(category, peer));
    }

    public Mono<Boolean> resetSaves() {
        return sendMain(ResetSaved.instance());
    }

    public Mono<List<SavedContact>> getSaved() {
        return sendMain(GetSaved.instance());
    }

    public Mono<Boolean> toggleTopPeers(boolean enabled) {
        return sendMain(ImmutableToggleTopPeers.of(enabled));
    }

    public Mono<Updates> addContact(AddContact request) {
        return sendMain(request);
    }

    public Mono<Updates> acceptContact(InputUser id) {
        return sendMain(ImmutableAcceptContact.of(id));
    }

    public Mono<Updates> getLocated(GetLocated request) {
        return sendMain(request);
    }

    public Mono<Updates> blockFromReplies(BlockFromReplies request) {
        return sendMain(request);
    }

    public Mono<ResolvedPeer> resolvePhone(String phone) {
        return sendMain(ImmutableResolvePhone.of(phone));
    }

    // photos namespace
    // ======================

    public Mono<Photo> updateProfilePhoto(InputPhoto photo) {
        return sendMain(ImmutableUpdateProfilePhoto.of(photo));
    }

    public Mono<Photo> updateProfilePhoto(String photoFileReferenceId) {
        return Mono.defer(() -> updateProfilePhoto(
                FileReferenceId.deserialize(photoFileReferenceId).asInputPhoto()));
    }

    public Mono<Photo> uploadProfilePhoto(UploadProfilePhoto request) {
        return sendMain(request);
    }

    public Mono<List<Long>> deletePhotos(Iterable<InputPhoto> photos) {
        return Mono.defer(() -> sendMain(ImmutableDeletePhotos.of(photos)));
    }

    @Compatible(Type.BOTH)
    public Mono<Photos> getUserPhotos(InputUser id, int offset, long maxId, int limit) {
        return sendMain(ImmutableGetUserPhotos.of(id, offset, maxId, limit));
    }
}
