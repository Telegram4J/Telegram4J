package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;

import java.util.Objects;

import static telegram4j.mtproto.util.CryptoUtil.sha1Digest;

/**
 * Authorization key holder with precomputed id, that's can be
 * used in {@link telegram4j.mtproto.MTProtoClient} implementation.
 */
public class AuthorizationKeyHolder {
    private final DataCenter dc;
    private final ByteBuf value;
    private final ByteBuf id;

    /**
     * Constructs a {@code AuthorizationKeyHolder} with given dc id, key and id.
     *
     * @param dc The id of associated datacenter.
     * @param value The auth key in the {@link ByteBuf}.
     * @param id The id of auth key in the {@link ByteBuf}.
     */
    public AuthorizationKeyHolder(DataCenter dc, ByteBuf value, ByteBuf id) {
        this.dc = Objects.requireNonNull(dc);
        this.value = value.asReadOnly();
        this.id = id.asReadOnly();
    }

    /**
     * Constructs a {@code AuthorizationKeyHolder} with given dc id and key.
     * And precomputes id of auth key.
     *
     * @param dc The id of associated datacenter.
     * @param value The auth key in the {@link ByteBuf}.
     */
    public AuthorizationKeyHolder(DataCenter dc, ByteBuf value) {
        this.dc = Objects.requireNonNull(dc);
        this.value = value.asReadOnly();
        ByteBuf authKeyHash = sha1Digest(value);
        this.id = authKeyHash.slice(authKeyHash.readableBytes() - 8, 8).asReadOnly();
    }

    /**
     * Gets associated with key dc id.
     *
     * @return The dc id.
     */
    public DataCenter getDc() {
        return dc;
    }

    /**
     * Gets auth key in read-only {@link ByteBuf}.
     *
     * @return The {@link ByteBuf} with auth key.
     */
    public ByteBuf getAuthKey() {
        return value;
    }

    /**
     * Gets auth key id in read-only {@link ByteBuf}.
     *
     * @return The {@link ByteBuf} with auth key id.
     */
    public ByteBuf getAuthKeyId() {
        return id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizationKeyHolder that = (AuthorizationKeyHolder) o;
        return dc.equals(that.dc) && value.equals(that.value) && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dc, value, id);
    }
}
