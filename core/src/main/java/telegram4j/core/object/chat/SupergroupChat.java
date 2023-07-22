package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.StickerSet;
import telegram4j.core.object.media.GeoPoint;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.mtproto.DcId;
import telegram4j.tl.*;
import telegram4j.tl.messages.AffectedHistory;
import telegram4j.tl.request.channels.ImmutableSetStickers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Subtype of channel, which represents a large group of 0-200,000 users.
 *
 * @see <a href="https://core.telegram.org/api/channel#supergroups">Supegroups</a>
 */
public final class SupergroupChat extends BaseChannel implements Channel, ChannelPeer {

    public SupergroupChat(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client, minData);
    }

    public SupergroupChat(MTProtoTelegramClient client, telegram4j.tl.ChannelFull fullData,
                          telegram4j.tl.Channel minData, @Nullable List<BotInfo> botInfo) {
        super(client, fullData, minData, botInfo);
    }

    @Override
    public Type getType() {
        return Type.SUPERGROUP;
    }

    @Override
    public Mono<BroadcastChannel> getLinkedChannel() {
        return super.getLinkedChannel()
                .cast(BroadcastChannel.class);
    }

    @Override
    public Mono<BroadcastChannel> getLinkedChannel(EntityRetrievalStrategy strategy) {
        return super.getLinkedChannel(strategy)
                .cast(BroadcastChannel.class);
    }

    // ChannelFull fields

    /**
     * Gets id of the group chat from which this supergroup was migrated,
     * if full data about chat is available and present.
     *
     * @see <a href="https://core.telegram.org/api/channel#migration">Chat Migration</a>
     * @return The id of the group chat from which this supergroup was migrated,
     * if full data about chat is available and present.
     */
    public Optional<Id> getMigratedFromChatId() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::migratedFromChatId)
                .map(Id::ofChat);
    }

    /**
     * Requests to retrieve chat from which channel was migrated.
     *
     * @return An {@link Mono} emitting on successful completion the {@link GroupChat original chat}.
     */
    public Mono<GroupChat> getMigratedFrom() {
        return getMigratedFrom(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve chat from which channel was migrated using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link GroupChat original chat}.
     */
    public Mono<GroupChat> getMigratedFrom(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getMigratedFromChatId())
                .flatMap(client.withRetrievalStrategy(strategy)::getChatById)
                .cast(GroupChat.class);
    }

    /**
     * Gets id of the latest message from which chat been migrated to the supergroup,
     * if full data about chat is available and present.
     *
     * @see <a href="https://core.telegram.org/api/channel#migration">Chat Migration</a>
     * @return The id of the latest message from which chat been migrated to the supergroup,
     * if full data about chat is available and present.
     */
    public Optional<Integer> getMigratedFromMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::migratedFromMaxId);
    }

    /**
     * Requests to retrieve the latest message from which channel was migrated.
     *
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getMigratedFromMaxMessage() {
        return getMigratedFromMaxMessage(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve the latest message from which channel was migrated using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getMigratedFromMaxMessage(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getMigratedFromMaxId())
                .flatMap(id -> client.withRetrievalStrategy(strategy)
                        .getMessages(getMigratedFromChatId().orElseThrow(),
                                List.of(ImmutableInputMessageID.of(id))));
    }

    /**
     * Gets geolocation of the supergroup, if full data about chat available and present.
     *
     * @see <a href="https://telegram.org/blog/contacts-local-groups">Location-Based Chats</a>
     * @return The {@link Location geolocation} of the supergroup, if full data about chat available and present.
     */
    public Optional<Location> getLocation() {
        if (fullData == null || !(fullData.location() instanceof BaseChannelLocation b)) {
            return Optional.empty();
        }
        return Optional.of(new Location(b));
    }

    /**
     * Gets send message interval for users in the supergroup, if full data about chat is available and present.
     *
     * @return The {@link Duration} with send message interval, if full data about chat is available and present.
     */
    public Optional<Duration> getSlowmodeDuration() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeSeconds)
                .map(Duration::ofSeconds);
    }

    /**
     * Gets next send message timestamp for <i>current</i> user, if full data about chat is available and present.
     *
     * @return The {@link Instant} of the next send message for <i>current</i> user, if full data about chat is available and present.
     */
    public Optional<Instant> getSlowmodeNextSendTimestamp() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeNextSendDate)
                .map(Instant::ofEpochSecond);
    }

    /**
     * Gets associated with this supergroup sticker set, if present
     * and if detailed information about supergroup is available.
     *
     * @return The associated sticker set, if present
     * and if detailed information about supergroup is available.
     */
    public Optional<StickerSet> getStickerSet() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::stickerset)
                .map(d -> new StickerSet(client, d));
    }

    /**
     * Requests to associate new stickerset with this group.
     *
     * @param stickerSetId The id of sticker set to associate.
     * @return A {@link Mono} emitting on successful completion boolean, indicates result.
     */
    public Mono<Boolean> setStickerSet(InputStickerSet stickerSetId) {
        Id id = getId();
        return client.asInputChannelExact(id)
                .flatMap(channel -> client.getMtProtoClientGroup().send(DcId.main(), ImmutableSetStickers.of(channel, stickerSetId)));
    }

    /**
     * Requests to unpin all messages in forum topic.
     *
     * @param topMessageId The id of the top message in topic.
     * @return A {@link Mono} emitting on successful completion {@link AffectedHistory} with affected history range.
     */
    public Mono<AffectedHistory> unpinAllMessages(int topMessageId) {
        return unpinAllMessages0(topMessageId);
    }

    @Override
    public String toString() {
        return "SupergroupChat{" +
                "minData=" + minData +
                ", fullData=" + fullData +
                '}';
    }

    /**
     * Geolocation representation for supergroups
     *
     * @see <a href="https://telegram.org/blog/contacts-local-groups">Location-Based chats</a>
     */
    public static final class Location {

        private final telegram4j.tl.BaseChannelLocation data;

        Location(BaseChannelLocation data) {
            this.data = data;
        }

        /**
         * Gets geo-point of the address.
         *
         * @return The {@link GeoPoint} of the address.
         */
        public GeoPoint getGeoPoint() {
            return new GeoPoint((BaseGeoPoint) data.geoPoint());
        }

        /**
         * Gets display description of the address.
         *
         * @return The display description of the address.
         */
        public String getAddress() {
            return data.address();
        }

        @Override
        public String toString() {
            return "Location{" +
                    "data=" + data +
                    '}';
        }
    }
}
