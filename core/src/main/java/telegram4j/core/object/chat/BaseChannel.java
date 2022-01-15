package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.tl.ChannelFull;

import java.util.Objects;
import java.util.Optional;

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
                "} " + super.toString();
    }
}
