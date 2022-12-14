package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.Reaction;
import telegram4j.core.object.User;
import telegram4j.core.object.*;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.core.util.PaginationSupport;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.file.Context;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.channels.BaseChannelParticipants;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static telegram4j.mtproto.util.TlEntityUtil.photoInputPeer;

abstract class BaseChannel extends BaseChat implements Channel {

    protected final telegram4j.tl.Channel minData;
    @Nullable
    protected final telegram4j.tl.ChannelFull fullData;
    @Nullable
    private final List<BotInfo> botInfo;

    protected BaseChannel(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = null;
        this.botInfo = null;
    }

    protected BaseChannel(MTProtoTelegramClient client,
                          ChannelFull fullData, telegram4j.tl.Channel minData,
                          @Nullable List<BotInfo> botInfo) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = Objects.requireNonNull(fullData);
        this.botInfo = botInfo;
    }

    @Override
    public Id getId() {
        Long acc = minData.min() ? null : minData.accessHash();
        return Id.ofChannel(minData.id(), acc);
    }

    @Override
    public Set<Flag> getFlags() {
        return Flag.of(fullData, minData);
    }

    @Override
    public String getName() {
        return minData.title();
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.ofNullable(minData.username());
    }

    @Override
    public List<Username> getUsernames() {
        var list = minData.usernames();
        return list != null ? list : List.of();
    }

    @Override
    public Optional<String> getAbout() {
        return Optional.ofNullable(fullData).map(ChannelFull::about);
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return Optional.ofNullable(fullData).map(ChannelFull::pinnedMsgId);
    }

    @Override
    public Mono<AuxiliaryMessages> getPinnedMessage(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(fullData)
                .mapNotNull(ChannelFull::pinnedMsgId)
                .flatMap(id -> client.withRetrievalStrategy(strategy)
                        .getMessages(getId(), List.of(ImmutableInputMessageID.of(id))));
    }

    @Override
    public Optional<ProfilePhoto> getMinPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.photo(), BaseChatPhoto.class))
                .map(d -> new ProfilePhoto(client, d, photoInputPeer(minData)));
    }

    @Override
    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.chatPhoto(), BasePhoto.class))
                .map(d -> new Photo(client, d, Context.createChatPhotoContext(photoInputPeer(minData), -1)));
    }

    @Override
    public Optional<Duration> getMessageAutoDeleteDuration() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::ttlPeriod)
                .map(Duration::ofSeconds);
    }

    @Override
    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(minData.date());
    }

    @Override
    public Optional<List<RestrictionReason>> getRestrictionReason() {
        return Optional.ofNullable(minData.restrictionReason());
    }

    @Override
    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::notifySettings)
                .map(PeerNotifySettings::new);
    }

    @Override
    public Optional<Integer> getParticipantsCount() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::participantsCount)
                // So far, I have not met such a thing that minData had a non-null participantsCount
                .or(() -> Optional.ofNullable(minData.participantsCount()));
    }

    @Override
    public Optional<Set<AdminRight>> getAdminRights() {
        return Optional.ofNullable(minData.adminRights()).map(AdminRight::of);
    }

    @Override
    public Optional<ChatRestrictions> getRestrictions() {
        return Optional.ofNullable(minData.bannedRights()).map(ChatRestrictions::new);
    }

    @Override
    public Optional<ChatRestrictions> getDefaultRestrictions() {
        return Optional.ofNullable(minData.defaultBannedRights()).map(ChatRestrictions::new);
    }

    @Override
    public Optional<Integer> getAvailableMinId() {
        return Optional.ofNullable(fullData).map(ChannelFull::availableMinId);
    }

    @Override
    public Mono<AuxiliaryMessages> getAvailableMinMessage(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getAvailableMinId())
                .flatMap(id -> client.withRetrievalStrategy(strategy)
                        .getMessages(getId(), List.of(ImmutableInputMessageID.of(id))));
    }

    @Override
    public Optional<Integer> getFolderId() {
        return Optional.ofNullable(fullData).map(ChannelFull::folderId);
    }

    @Override
    public Optional<List<String>> getPendingSuggestions() {
        return Optional.ofNullable(fullData).map(ChannelFull::pendingSuggestions);
    }

    @Override
    public Optional<Integer> getPendingRequests() {
        return Optional.ofNullable(fullData).map(ChannelFull::requestsPending);
    }

    @Override
    public Optional<Id> getDefaultSendAs() {
        return Optional.ofNullable(fullData).map(ChannelFull::defaultSendAs).map(Id::of);
    }

    @Override
    public Optional<Set<Id>> getRecentRequestersIds() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::recentRequesters)
                .map(list -> list.stream()
                        .map(Id::ofUser)
                        .collect(Collectors.toSet()));
    }

    @Override
    public Flux<User> getRecentRequesters(EntityRetrievalStrategy strategy) {
        var retriever = client.withRetrievalStrategy(strategy);
        return Mono.justOrEmpty(fullData)
                .mapNotNull(ChannelFull::recentRequesters)
                .flatMapIterable(Function.identity())
                .flatMap(id -> retriever.getUserById(Id.ofUser(id)));
    }

    @Override
    public Optional<Variant2<Boolean, List<Reaction>>> getAvailableReactions() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::availableReactions)
                .map(EntityFactory::createChatReactions);
    }

    @Override
    public Optional<Integer> getPts() {
        return Optional.ofNullable(fullData).map(ChannelFull::pts);
    }

    @Override
    public Optional<Integer> getReadInboxMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::readInboxMaxId);
    }

    @Override
    public Optional<Integer> getReadOutboxMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::readOutboxMaxId);
    }

    @Override
    public Optional<Integer> getUnreadCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::unreadCount);
    }

    @Override
    public Optional<Integer> getAdminsCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::adminsCount);
    }

    @Override
    public Optional<Integer> getKickedCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::kickedCount);
    }

    @Override
    public Optional<Integer> getBannedCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::bannedCount);
    }

    @Override
    public Optional<Integer> getOnlineCount() {
        return Optional.ofNullable(fullData).map(ChannelFull::onlineCount);
    }

    @Override
    public Optional<Id> getLinkedChannelId() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::linkedChatId)
                .map(Id::ofChannel);
    }

    @Override
    public Mono<? extends Channel> getLinkedChannel(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getLinkedChannelId())
                .flatMap(id -> client.withRetrievalStrategy(strategy).getChatById(id))
                .cast(Channel.class);
    }

    @Override
    public Optional<String> getThemeEmoticon() {
        return Optional.ofNullable(fullData).map(ChannelFull::themeEmoticon);
    }

    @Override
    public Optional<InputGroupCall> getCall() {
        return Optional.ofNullable(fullData).map(ChannelFull::call);
    }

    @Override
    public Optional<Id> getGroupCallDefaultJoinAsId() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::groupcallDefaultJoinAs)
                .map(Id::of);
    }

    @Override
    public Optional<ExportedChatInvite> getExportedInvite() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.mapCast(d.exportedInvite(), ChatInviteExported.class))
                .map(d -> new ExportedChatInvite(client, d));
    }

    @Override
    public Optional<Integer> getStatsDcId() {
        return Optional.ofNullable(fullData).map(ChannelFull::statsDc);
    }

    @Override
    public Optional<List<BotInfo>> getBotInfo() {
        return Optional.ofNullable(botInfo);
    }

    @Override
    public Mono<Void> editTitle(String newTitle) {
        Id id = getId();
        return client.asInputChannel(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editTitle(channel, newTitle));
    }

    @Override
    public Mono<Channel> editAdmin(Id userId, Iterable<AdminRight> rights, String rank) {
        Id id = getId();
        return client.asInputChannel(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .zipWith(client.asInputUser(userId)
                        .switchIfEmpty(MappingUtil.unresolvedPeer(userId)))
                .flatMap(TupleUtils.function((channel, peer) -> client.getServiceHolder().getChatService()
                        .editAdmin(channel, peer, ImmutableChatAdminRights.of(
                                MappingUtil.getMaskValue(rights)), rank)))
                .mapNotNull(c -> EntityFactory.createChat(client, c, null))
                .cast(Channel.class);
    }

    @Override
    public Mono<Channel> editBanned(Id peerId, Iterable<ChatRestrictions.Right> rights, @Nullable Instant untilTimestamp) {
        int untilDate = untilTimestamp != null ? Math.toIntExact(untilTimestamp.getEpochSecond()) : 0;
        Id id = getId();
        return client.asInputChannel(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .zipWith(client.asInputPeer(peerId)
                        .switchIfEmpty(MappingUtil.unresolvedPeer(peerId)))
                .flatMap(TupleUtils.function((channel, peer) -> client.getServiceHolder().getChatService()
                        .editBanned(channel, peer, ImmutableChatBannedRights.of(
                                MappingUtil.getMaskValue(rights), untilDate))))
                .mapNotNull(c -> EntityFactory.createChat(client, c, null))
                .cast(Channel.class);
    }

    @Override
    public Mono<Void> editPhoto(@Nullable BaseInputPhoto spec) {
        var photo = spec != null ? ImmutableBaseInputChatPhoto.of(spec) : InputChatPhotoEmpty.instance();
        Id id = getId();
        return client.asInputChannel(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editPhoto(channel, photo));
    }

    @Override
    public Mono<Void> editPhoto(@Nullable InputChatUploadedPhoto spec) {
        var photo = spec != null ? spec : InputChatPhotoEmpty.instance();
        Id id = getId();
        return client.asInputChannel(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editPhoto(channel, photo));
    }

    @Override
    public Mono<Void> leave() {
        Id id = getId();
        return client.asInputChannel(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .flatMap(client.getServiceHolder().getChatService()::leaveChannel);
    }

    @Override
    public Mono<ChatParticipant> getParticipantById(EntityRetrievalStrategy strategy, Id peerId) {
        return client.withRetrievalStrategy(strategy).getParticipantById(getId(), peerId);
    }

    @Override
    public Flux<ChatParticipant> getParticipants(ChannelParticipantsFilter filter, int offset, int limit) {
        Id id = getId();
        return client.asInputChannel(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .flatMapMany(channel -> PaginationSupport.paginate(o -> client.getServiceHolder().getChatService()
                                .getParticipants(channel, filter, o, limit, 0), BaseChannelParticipants::count, offset, limit)
                        .flatMap(data -> {
                            var chats = data.chats().stream()
                                    .map(c -> (Channel) EntityFactory.createChat(client, c, null))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));
                            var users = data.users().stream()
                                    .map(u -> EntityFactory.createUser(client, u))
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                            return Flux.fromIterable(data.participants())
                                    .map(c -> {
                                        Id peerId = Id.of(TlEntityUtil.getUserId(c));
                                        MentionablePeer peer;
                                        switch (peerId.getType()) {
                                            case USER:
                                                peer = users.get(peerId);
                                                break;
                                            case CHANNEL:
                                                peer = chats.get(peerId);
                                                break;
                                            default:
                                                throw new IllegalStateException();
                                        }

                                        return new ChatParticipant(client, peer, c,
                                                Id.of(channel, client.getSelfId()));
                                    });
                        }));

    }

    @Override
    public Mono<Boolean> editAbout(String newAbout) {
        Id id = getId();
        return client.asInputPeer(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editChatAbout(channel, newAbout));
    }
}
