package telegram4j.core.object.chat;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.ChannelForbidden;

import java.time.Instant;
import java.util.Optional;

public final class UnavailableChannel extends BaseUnavailableChat implements ChannelPeer, UnavailableChat {
    private final ChannelForbidden data;

    public UnavailableChannel(MTProtoTelegramClient client, ChannelForbidden data) {
        super(client);
        this.data = data;
    }

    @Override
    public Id getId() {
        return Id.ofChannel(data.id(), data.accessHash());
    }

    @Override
    public Type getType() {
        return data.broadcast() ? Type.CHANNEL : Type.SUPERGROUP;
    }

    @Override
    public String getName() {
        return data.title();
    }

    public Optional<Instant> getUntilTimestamp() {
        return Optional.ofNullable(data.untilDate())
                .map(Instant::ofEpochSecond);
    }

    @Override
    public String toString() {
        return "UnavailableChannel{" +
                "data=" + data +
                '}';
    }
}
