package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputPeer;
import telegram4j.tl.TlSerializer;

public class ProfilePhotoContext extends Context {
    protected final InputPeer peer;

    ProfilePhotoContext(InputPeer peer) {
        this.peer = peer;
    }

    @Override
    public Type getType() {
        return Type.PROFILE_PHOTO;
    }

    public InputPeer getPeer() {
        return peer;
    }

    @Override
    void serialize(ByteBuf buf) {
        TlSerializer.serialize(buf, peer);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfilePhotoContext that = (ProfilePhotoContext) o;
        return peer.equals(that.peer);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + peer.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "ProfilePhotoContext{" +
                "peer=" + peer +
                '}';
    }
}
