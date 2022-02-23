package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ChannelLocation;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChannelLocation;
import telegram4j.tl.ChannelFull;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Subtype of channel, which represents a large group of 0-200,000 users. */
public final class SupergroupChat extends BaseChannel {

    public SupergroupChat(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client, Id.ofChannel(minData.id(), minData.accessHash()), Type.SUPERGROUP, minData);
    }

    public SupergroupChat(MTProtoTelegramClient client, telegram4j.tl.ChannelFull fullData,
                          telegram4j.tl.Channel minData, @Nullable ExportedChatInvite exportedChatInvite) {
        super(client, Id.ofChannel(minData.id(), minData.accessHash()), Type.SUPERGROUP, fullData, minData, exportedChatInvite);
    }

    // ChannelFull fields

    public Optional<Id> getMigratedFromChatId() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::migratedFromChatId)
                .map(Id::ofChat);
    }

    public Optional<Integer> getMigratedFromMaxId() {
        return Optional.ofNullable(fullData).map(ChannelFull::migratedFromMaxId);
    }

    public Optional<ChannelLocation> getLocation() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.location(), BaseChannelLocation.class))
                .map(d -> new ChannelLocation(client, d));
    }

    public Optional<Duration> getSlowmodeDuration() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeSeconds)
                .map(Duration::ofSeconds);
    }

    public Optional<Instant> getSlowmodeNextSendTimestamp() {
        return Optional.ofNullable(fullData)
                .map(ChannelFull::slowmodeNextSendDate)
                .map(Instant::ofEpochSecond);
    }

    @Override
    public String toString() {
        return "SupergroupChat{} " + super.toString();
    }
}
