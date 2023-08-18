/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.User;
import telegram4j.core.object.*;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.core.util.PaginationSupport;
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
import java.util.stream.Stream;

import static telegram4j.mtproto.util.TlEntityUtil.photoInputPeer;

sealed abstract class BaseChannel extends BaseChat implements Channel
        permits SupergroupChat, BroadcastChannel {

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
        return minData.photo() instanceof BaseChatPhoto p
                ? Optional.of(new ProfilePhoto(client, p, photoInputPeer(minData)))
                : Optional.empty();
    }

    @Override
    public Optional<Photo> getPhoto() {
        if (fullData == null || !(fullData.chatPhoto() instanceof BasePhoto p)) {
            return Optional.empty();
        }
        return Optional.of(new Photo(client, p, Context.createChatPhotoContext(photoInputPeer(minData), -1)));
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
    public List<RestrictionReason> getRestrictionReasons() {
        var res = minData.restrictionReason();
        return res != null ? res : List.of();
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
    public Optional<ChatReactions> getAvailableReactions() {
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
        return Optional.ofNullable(fullData).map(d -> new ExportedChatInvite(client, (ChatInviteExported) d));
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
        return client.asInputChannelExact(id)
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editTitle(channel, newTitle));
    }

    @Override
    public Mono<Channel> editAdmin(Id userId, Iterable<AdminRight> rights, String rank) {
        Id id = getId();
        return Mono.zip(client.asInputChannelExact(id), client.asInputUserExact(userId))
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
        return Mono.zip(client.asInputChannelExact(id), client.asInputPeerExact(peerId))
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
        return client.asInputChannelExact(id)
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editPhoto(channel, photo));
    }

    @Override
    public Mono<Void> editPhoto(@Nullable InputChatUploadedPhoto spec) {
        var photo = spec != null ? spec : InputChatPhotoEmpty.instance();
        Id id = getId();
        return client.asInputChannelExact(id)
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editPhoto(channel, photo));
    }

    @Override
    public Mono<Void> leave() {
        Id id = getId();
        return client.asInputChannelExact(id)
                .flatMap(client.getServiceHolder().getChatService()::leaveChannel);
    }

    @Override
    public Mono<ChatParticipant> getParticipantById(EntityRetrievalStrategy strategy, Id peerId) {
        return client.withRetrievalStrategy(strategy)
                .getParticipantById(getId(), peerId);
    }

    @Override
    public Flux<ChatParticipant> getParticipants(ChannelParticipantsFilter filter, int offset, int limit) {
        Id id = getId();
        return client.asInputChannelExact(id)
                .flatMapMany(channel -> PaginationSupport.paginate(o -> client.getServiceHolder().getChatService()
                                .getParticipants(channel, filter, o, limit, 0), BaseChannelParticipants::count, offset, limit)
                        .flatMap(data -> {
                            var chats = data.chats().stream()
                                    .flatMap(c -> Stream.ofNullable((Channel) EntityFactory.createChat(client, c, null)))
                                    .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));
                            var users = data.users().stream()
                                    .flatMap(u -> Stream.ofNullable(EntityFactory.createUser(client, u)))
                                    .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                            return Flux.fromIterable(data.participants())
                                    .map(c -> {
                                        Id peerId = Id.of(TlEntityUtil.getUserId(c));
                                        var peer = switch (peerId.getType()) {
                                            case USER -> users.get(peerId);
                                            case CHANNEL -> chats.get(peerId);
                                            case CHAT -> throw new IllegalStateException();
                                        };

                                        return new ChatParticipant(client, peer, c,
                                                Id.of(channel, client.getSelfId()));
                                    });
                        }));

    }

    @Override
    public Mono<Boolean> editAbout(String newAbout) {
        Id id = getId();
        return client.asInputPeerExact(id)
                .flatMap(channel -> client.getServiceHolder().getChatService()
                        .editChatAbout(channel, newAbout));
    }
}
