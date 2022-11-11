package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.Peer;
import telegram4j.tl.TlSerializer;

public class MessageMediaContext extends Context {
    private final Peer chatPeer;
    private final int messageId;

    MessageMediaContext(Peer chatPeer, int messageId) {
        this.chatPeer = chatPeer;
        this.messageId = messageId;
    }

    public Peer getChatPeer() {
        return chatPeer;
    }

    public int getMessageId() {
        return messageId;
    }

    @Override
    public Type getType() {
        return Type.MESSAGE_MEDIA;
    }

    @Override
    void serialize(ByteBuf buf) {
        TlSerializer.serialize(buf, chatPeer);
        buf.writeIntLE(messageId);
    }

    @Override
    public String toString() {
        return "MessageMediaContext{" +
                "chatPeer=" + chatPeer +
                ", messageId=" + messageId +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaContext that = (MessageMediaContext) o;
        return messageId == that.messageId && chatPeer.equals(that.chatPeer);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + chatPeer.hashCode();
        h += (h << 5) + messageId;
        return h;
    }
}