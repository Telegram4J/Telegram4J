package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.Peer;

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

public final class BotInfoContext extends Context {
    private final Peer chatPeer;
    private final long botId; // may be -1 if it's equals to chatPeer

    BotInfoContext(Peer chatPeer, long botId) {
        this.chatPeer = chatPeer;
        this.botId = botId;
    }

    public Peer getChatPeer() {
        return chatPeer;
    }

    public long getBotId() {
        return botId == -1 ? getRawPeerId(chatPeer) : botId;
    }

    @Override
    public Type getType() {
        return Type.BOT_INFO;
    }

    @Override
    void serialize(ByteBuf buf) {
        serializePeer(buf, chatPeer);
        if (botId != -1) {
            buf.writeByte(1);
            buf.writeLongLE(botId);
        } else {
            buf.writeByte(0);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BotInfoContext that = (BotInfoContext) o;
        return botId == that.botId && chatPeer.equals(that.chatPeer);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + chatPeer.hashCode();
        h += (h << 5) + Long.hashCode(botId);
        return h;
    }

    @Override
    public String toString() {
        return "BotInfoContext{" +
                "chatPeer=" + chatPeer +
                ", botId=" + botId +
                '}';
    }
}
