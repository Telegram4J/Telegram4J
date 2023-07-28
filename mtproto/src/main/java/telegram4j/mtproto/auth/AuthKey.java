package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.internal.Crypto;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.api.TlEncodingUtil;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

/** Value-based tuple of auth key and it's details. */
public final class AuthKey {
    private final ByteBuf value;
    private final long id;
    @Nullable
    private final Instant expiresAtTimestamp;

    public AuthKey(ByteBuf value, @Nullable Instant expiresAtTimestamp) {
        ByteBuf copy = TlEncodingUtil.copyAsUnpooled(value);
        this.value = copy;
        ByteBuf hash = sha1Digest(copy);
        this.id = hash.getLongLE(hash.readableBytes() - 8);
        this.expiresAtTimestamp = expiresAtTimestamp;
    }

    // SHA1 rarely used and there is no reason to store it in global TL. Only for IO threads
    static MessageDigest getSHA1() {
        var localOrNew = Crypto.SHA1.getIfExists();
        if (localOrNew == null) {
            localOrNew = CryptoUtil.createDigest("SHA-1");
        } else {
            localOrNew.reset();
        }
        return localOrNew;
    }

    public static ByteBuf sha1Digest(ByteBuf buf) {
        var sha1 = getSHA1();
        sha1.update(buf.nioBuffer());
        return Unpooled.wrappedBuffer(sha1.digest());
    }

    public AuthKey(ByteBuf value) {
        this(value, null);
    }

    public ByteBuf value() {
        return value;
    }

    /** {@return SHA-1 of last 8 bytes of the auth key} */
    public long id() {
        return id;
    }

    /** {@return An timestamp of key expiration} If auth key is temporary */
    public Optional<Instant> expiresAtTimestamp() {
        return Optional.ofNullable(expiresAtTimestamp);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthKey that)) return false;
        return id == that.id && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        String expireTimestamp = expiresAtTimestamp != null ? (", expiresAtTimestamp=" + expiresAtTimestamp) : "";
        return "AuthKey{id=0x" + Long.toHexString(id) + expireTimestamp + '}';
    }
}
