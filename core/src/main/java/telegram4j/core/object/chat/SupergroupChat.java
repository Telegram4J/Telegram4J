package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ChannelLocation;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.util.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChannelLocation;
import telegram4j.tl.ChannelFull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Subtype of channel, which represents a large group of 0-200,000 users.
 *
 * @see <a href="https://core.telegram.org/api/channel#supergroups">Supegroups</a>
 */
public final class SupergroupChat extends BaseChannel {

    public SupergroupChat(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client, minData);
    }

    public SupergroupChat(MTProtoTelegramClient client, telegram4j.tl.ChannelFull fullData,
                          telegram4j.tl.Channel minData, @Nullable ExportedChatInvite exportedChatInvite) {
        super(client, fullData, minData, exportedChatInvite);
    }

    @Override
    public Type getType() {
        return Type.SUPERGROUP;
    }

    // ChannelFull fields

    /**
     * Gets id of the group chat from which this supergroup was migrated, if present.
     *
     * @see <a href="https://core.telegram.org/api/channel#migration">Chat Migration</a>
     * @return The id of the group chat from which this supergroup was migrated, if present.
     */
    public Optional<Id> getMigratedFromChatId() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::migratedFromChatId)
                .map(Id::ofChat);
    }

    /**
     * Gets id of the latest message from which chat been migrated to the supergroup, if present.
     *
     * @see <a href="https://core.telegram.org/api/channel#migration">Chat Migration</a>
     * @return The id of the latest message from which chat been migrated to the supergroup, if present.
     */
    public Optional<Integer> getMigratedFromMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::migratedFromMaxId);
    }

    /**
     * Gets geolocation of the supergroup, if present.
     *
     * @see <a href="https://telegram.org/blog/contacts-local-groups">Location-Based Chats</a>
     * @return The {@link ChannelLocation geolocation} of the supergroup, if present.
     */
    public Optional<ChannelLocation> getLocation() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.location(), BaseChannelLocation.class))
                .map(d -> new ChannelLocation(client, d));
    }

    /**
     * Gets send message interval for users in the supergroup, if present.
     *
     * @return The {@link Duration} with send message interval, if present.
     */
    public Optional<Duration> getSlowmodeDuration() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeSeconds)
                .map(Duration::ofSeconds);
    }

    /**
     * Gets next send message timestamp for <i>current</i> user, if present.
     *
     * @return The {@link Instant} of the next send message for <i>current</i> user, if present.
     */
    public Optional<Instant> getSlowmodeNextSendTimestamp() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeNextSendDate)
                .map(Instant::ofEpochSecond);
    }

    @Override
    public String toString() {
        return "SupergroupChat{" +
                "minData=" + minData +
                ", fullData=" + fullData +
                '}';
    }
}
