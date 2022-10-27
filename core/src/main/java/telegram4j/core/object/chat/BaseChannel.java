package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
import telegram4j.core.object.StickerSet;
import telegram4j.core.object.*;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.core.util.PaginationSupport;
import telegram4j.core.util.Variant2;
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

import static telegram4j.mtproto.util.TlEntityUtil.toInputChannel;

abstract class BaseChannel extends BaseChat implements Channel {

    protected final telegram4j.tl.Channel minData;
    @Nullable
    protected final telegram4j.tl.ChannelFull fullData;
    @Nullable
    protected final ExportedChatInvite exportedChatInvite;

    protected BaseChannel(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = null;
        this.exportedChatInvite = null;
    }

    protected BaseChannel(MTProtoTelegramClient client,
                          ChannelFull fullData, telegram4j.tl.Channel minData,
                          @Nullable ExportedChatInvite exportedChatInvite) {
        super(client);
        this.minData = Objects.requireNonNull(minData);
        this.fullData = Objects.requireNonNull(fullData);
        this.exportedChatInvite = exportedChatInvite;
    }

    @Override
    public Id getId() {
        return Id.ofChannel(minData.id(), minData.accessHash());
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
                        .getMessagesById(getId(), List.of(ImmutableInputMessageID.of(id))));
    }

    @Override
    public Optional<ProfilePhoto> getMinPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.photo(), BaseChatPhoto.class))
                .map(d -> new ProfilePhoto(client, d, client.asResolvedInputPeer(getId()), -1));
    }

    @Override
    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.chatPhoto(), BasePhoto.class))
                .map(d -> new Photo(client, d, -1, client.asResolvedInputPeer(getId())));
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
    public Optional<List<BotInfo>> getBotInfo() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::botInfo)
                .map(list -> list.stream()
                        .map(d -> new BotInfo(client, d))
                        .collect(Collectors.toList()));
    }

    @Override
    public Optional<StickerSet> getStickerSet() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::stickerset)
                .map(d -> new StickerSet(client, d));
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
                // So far, I have not met such a thing that ChannelMin had a non-null participantsCount
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
    public Optional<Integer> getFolderId() {
        return Optional.ofNullable(fullData).map(ChannelFull::folderId);
    }

    @Override
    public Optional<List<String>> getPendingSuggestions() {
        return Optional.ofNullable(fullData).map(ChannelFull::pendingSuggestions);
    }

    @Override
    public Optional<Integer> getRequestsPending() {
        return Optional.ofNullable(fullData).map(ChannelFull::requestsPending);
    }

    @Override
    public Optional<Id> getDefaultSendAs() {
        return Optional.ofNullable(fullData).map(ChannelFull::defaultSendAs).map(Id::of);
    }

    @Override
    public Optional<List<Id>> getRecentRequesters() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::recentRequesters)
                .map(list -> list.stream()
                        .map(l -> Id.ofUser(l, null))
                        .collect(Collectors.toList()));
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
                .map(id -> Id.ofChannel(id, null));
    }

    @Override
    public Mono<Channel> getLinkedChannel(EntityRetrievalStrategy strategy) {
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
    public Optional<Id> getGroupCallDefaultJoinAs() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::groupcallDefaultJoinAs)
                .map(Id::of);
    }

    @Override
    public Optional<ExportedChatInvite> getExportedInvite() {
        return Optional.ofNullable(exportedChatInvite);
    }

    @Override
    public Optional<Integer> getStatsDcId() {
        return Optional.ofNullable(fullData).map(ChannelFull::statsDc);
    }

    @Override
    public Mono<Void> editTitle(String newTitle) {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));

        return client.getServiceHolder().getChatService()
                .editTitle(channel, newTitle);
    }

    @Override
    public Mono<Channel> editAdmin(Id userId, Iterable<AdminRight> rights, String rank) {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));

        return client.asInputUser(userId)
                .switchIfEmpty(MappingUtil.unresolvedPeer(userId))
                .flatMap(target -> client.getServiceHolder().getChatService()
                        .editAdmin(channel, target, ImmutableChatAdminRights.of(MappingUtil.getMaskValue(rights)), rank))
                .mapNotNull(c -> EntityFactory.createChat(client, c, null))
                .cast(Channel.class);
    }

    @Override
    public Mono<Channel> editBanned(Id peerId, Iterable<ChatRestrictions.Right> rights, @Nullable Instant untilTimestamp) {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));
        int untilDate = untilTimestamp != null ? Math.toIntExact(untilTimestamp.getEpochSecond()) : 0;

        return client.asInputPeer(peerId)
                .switchIfEmpty(MappingUtil.unresolvedPeer(peerId))
                .flatMap(target -> client.getServiceHolder().getChatService()
                        .editBanned(channel, target, ImmutableChatBannedRights.of(MappingUtil.getMaskValue(rights), untilDate)))
                .mapNotNull(c -> EntityFactory.createChat(client, c, null))
                .cast(Channel.class);
    }

    @Override
    public Mono<Void> editPhoto(@Nullable BaseInputPhoto photo) {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));

        return Mono.justOrEmpty(photo)
                .<InputChatPhoto>map(ImmutableBaseInputChatPhoto::of)
                .defaultIfEmpty(InputChatPhotoEmpty.instance())
                .flatMap(c -> client.getServiceHolder().getChatService()
                        .editPhoto(channel, c));
    }

    @Override
    public Mono<Void> editPhoto(@Nullable InputChatUploadedPhoto spec) {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));

        return Mono.<InputChatPhoto>justOrEmpty(spec)
                .defaultIfEmpty(InputChatPhotoEmpty.instance())
                .flatMap(c -> client.getServiceHolder().getChatService()
                        .editPhoto(channel, c));
    }

    @Override
    public Mono<Void> leave() {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));

        return client.getServiceHolder().getChatService()
                .leaveChannel(channel);
    }

    @Override
    public Mono<Boolean> setStickers(InputStickerSet stickerSetId) {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));

        return client.getServiceHolder().getChatService()
                .setStickers(channel, stickerSetId);
    }

    @Override
    public Mono<ChatParticipant> getParticipant(Id participantId) {
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(getId()));

        return client.asInputPeer(participantId)
                .switchIfEmpty(MappingUtil.unresolvedPeer(participantId))
                .flatMap(participantPeer -> client.getServiceHolder()
                        .getChatService().getParticipant(channel, participantPeer))
                .map(d -> {
                    Peer peerId = TlEntityUtil.getUserId(d.participant());
                    PeerEntity peerEntity;
                    switch (peerId.identifier()) {
                        case PeerChat.ID:
                        case PeerChannel.ID:
                            peerEntity = d.chats().stream()
                                    .filter(u -> u.id() == participantId.asLong())
                                    .findFirst()
                                    .map(u -> Objects.requireNonNull(EntityFactory.createChat(client, u, null)))
                                    .orElseThrow();
                            break;
                        case PeerUser.ID:
                            peerEntity = d.users().stream()
                                    .filter(u -> u.id() == participantId.asLong())
                                    .findFirst()
                                    .map(u -> Objects.requireNonNull(EntityFactory.createUser(client, u)))
                                    .orElseThrow();
                            break;
                        default: throw new IllegalArgumentException("Unknown Peer type: " + peerId);
                    }

                    return new ChatParticipant(client, peerEntity, d.participant(), getId());
                });
    }

    @Override
    public Flux<ChatParticipant> getParticipants(ChannelParticipantsFilter filter, int offset, int limit) {
        Id id = getId();
        InputChannel channel = toInputChannel(client.asResolvedInputPeer(id));

        return PaginationSupport.paginate(o -> client.getServiceHolder().getChatService()
                .getParticipants(channel, filter, o, limit, 0)
                .cast(BaseChannelParticipants.class), BaseChannelParticipants::count, offset, limit)
                .flatMap(data -> {
                    var chats = data.chats().stream()
                            .map(c -> EntityFactory.createChat(client, c, null))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));
                    var users = data.users().stream()
                            .map(u -> EntityFactory.createUser(client, u))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                    return Flux.fromIterable(data.participants())
                            .map(c -> {
                                Id peerId = Id.of(TlEntityUtil.getUserId(c));
                                PeerEntity peerEntity;
                                switch (peerId.getType()) {
                                    case USER:
                                        peerEntity = users.get(peerId);
                                        break;
                                    case CHAT:
                                    case CHANNEL:
                                        peerEntity = chats.get(peerId);
                                        break;
                                    default:
                                        throw new IllegalStateException();
                                }

                                return new ChatParticipant(client, peerEntity, c, id);
                            });
                });
    }

    @Override
    public Mono<Boolean> editAbout(String newAbout) {
        InputPeer channel = client.asResolvedInputPeer(getId());

        return client.getServiceHolder().getChatService()
                .editChatAbout(channel, newAbout);
    }
}
