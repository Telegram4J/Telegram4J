package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.api.TlEncodingUtil;

import static telegram4j.mtproto.util.CryptoUtil.sha1Digest;

public final class AuthKey {
    private final ByteBuf value;
    private final long id;

    public AuthKey(ByteBuf value) {
        ByteBuf copy = TlEncodingUtil.copyAsUnpooled(value);
        this.value = copy;
        ByteBuf hash = sha1Digest(copy);
        this.id = hash.getLongLE(hash.readableBytes() - 8);
    }

    public ByteBuf value() {
        return value;
    }

    public long id() {
        return id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthKey that)) return false;
        return id == that.id && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
