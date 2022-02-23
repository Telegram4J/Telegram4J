package telegram4j.core.object.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Id;

/** Represents an unlimited channel with messages broadcasting. */
public final class BroadcastChannel extends BaseChannel {

    public BroadcastChannel(MTProtoTelegramClient client, telegram4j.tl.Channel minData) {
        super(client, Id.ofChannel(minData.id(), minData.accessHash()), Type.CHANNEL, minData);
    }

    public BroadcastChannel(MTProtoTelegramClient client, telegram4j.tl.ChannelFull fullData,
                            telegram4j.tl.Channel minData, @Nullable ExportedChatInvite exportedChatInvite) {
        super(client, Id.ofChannel(minData.id(), minData.accessHash()), Type.CHANNEL, fullData, minData, exportedChatInvite);
    }

    @Override
    public String toString() {
        return "BroadcastChannel{} " + super.toString();
    }
}
