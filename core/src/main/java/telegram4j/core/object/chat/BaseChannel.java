package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.*;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChatPhoto;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.ChannelFull;
import telegram4j.tl.ImmutableInputPeerChannel;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

abstract class BaseChannel extends BaseChat implements Channel {

    protected final telegram4j.tl.Channel minData;
    @Nullable
    protected final telegram4j.tl.ChannelFull fullData;

    protected BaseChannel(MTProtoTelegramClient client, Id id, Type type, telegram4j.tl.Channel minData) {
        super(client, id, type);
        this.minData = Objects.requireNonNull(minData, "minData");
        this.fullData = null;
    }

    protected BaseChannel(MTProtoTelegramClient client, Id id, Type type,
                          telegram4j.tl.ChannelFull fullData, telegram4j.tl.Channel minData) {
        super(client, id, type);
        this.minData = Objects.requireNonNull(minData, "minData");
        this.fullData = Objects.requireNonNull(fullData, "fullData");
    }

    @Override
    public String getTitle() {
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
    public Optional<ChatPhoto> getMinPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(minData.photo(), BaseChatPhoto.class))
                .map(d -> new ChatPhoto(client, d, getIdAsPeer(), -1));
    }

    @Override
    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(fullData)
                .map(d -> TlEntityUtil.unmapEmpty(d.chatPhoto(), BasePhoto.class))
                .map(d -> new Photo(client, d, ImmutableInputPeerChannel.of(minData.id(),
                        Objects.requireNonNull(minData.accessHash())), -1));
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
        return Optional.ofNullable(minData.restrictionReason())
                .map(list -> list.stream()
                        .map(d -> new RestrictionReason(client, d))
                        .collect(Collectors.toList()));
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
                .map(d -> new PeerNotifySettings(client, d));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseChannel)) return false;
        BaseChannel that = (BaseChannel) o;
        return minData.equals(that.minData) && Objects.equals(fullData, that.fullData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minData, fullData);
    }

    @Override
    public String toString() {
        return "BaseChannel{" +
                "minData=" + minData +
                ", fullData=" + fullData +
                '}';
    }
}
