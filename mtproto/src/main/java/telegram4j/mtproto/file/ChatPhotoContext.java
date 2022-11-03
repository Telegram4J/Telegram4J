package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputPeer;

import java.util.Optional;

public class ChatPhotoContext extends ProfilePhotoContext {
    private final int messageId; // can be -1 for absent values

    ChatPhotoContext(InputPeer peer, int messageId) {
        super(peer);
        this.messageId = messageId;
    }

    @Override
    public Type getType() {
        return Type.CHAT_PHOTO;
    }

    public Optional<Integer> getMessageId() {
        return messageId == -1 ? Optional.empty() : Optional.of(messageId);
    }

    @Override
    void serialize(ByteBuf buf) {
        super.serialize(buf);
        if (messageId != -1) {
            buf.writeIntLE(messageId);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ChatPhotoContext that = (ChatPhotoContext) o;
        return messageId == that.messageId;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + messageId;
        h += (h << 5) + peer.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "ChatPhotoContext{" +
                "messageId=" + messageId +
                ", peer=" + peer +
                '}';
    }
}
