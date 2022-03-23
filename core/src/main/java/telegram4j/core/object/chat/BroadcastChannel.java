package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ExportedChatInvite;

/**
 * Represents an unlimited channel with messages broadcasting.
 *
 * @see <a href="https://core.telegram.org/api/channel#channels">Broadcasting Channels</a>
 */
public final class BroadcastChannel extends BaseChannel {

    public BroadcastChannel(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client, minData);
    }

    public BroadcastChannel(MTProtoTelegramClient client, telegram4j.tl.ChannelFull fullData,
                            telegram4j.tl.Channel minData, @Nullable ExportedChatInvite exportedChatInvite) {
        super(client, fullData, minData, exportedChatInvite);
    }

    @Override
    public Type getType() {
        return Type.CHANNEL;
    }

    @Override
    public String toString() {
        return "BroadcastChannel{" +
                "minData=" + minData +
                ", fullData=" + fullData +
                '}';
    }
}
