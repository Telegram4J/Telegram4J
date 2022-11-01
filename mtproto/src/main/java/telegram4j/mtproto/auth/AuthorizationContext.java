package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import telegram4j.tl.mtproto.ServerDHParams;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.util.ReferenceCountUtil.safeRelease;

/** Holder object used during authorization key generation. */
public final class AuthorizationContext {
    private volatile ByteBuf nonce;
    private volatile ByteBuf newNonce;
    private volatile ByteBuf serverNonce;
    private volatile ByteBuf authKey;
    private volatile long serverSalt;
    private volatile ByteBuf authAuxHash;
    private volatile ServerDHParams serverDHParams;
    private volatile int timeOffset;
    private final AtomicInteger retry = new AtomicInteger();

    public ByteBuf getNonce() {
        return nonce;
    }

    public void setNonce(ByteBuf nonce) {
        this.nonce = Objects.requireNonNull(nonce);
    }

    public ByteBuf getNewNonce() {
        return newNonce;
    }

    public void setNewNonce(ByteBuf newNonce) {
        this.newNonce = Objects.requireNonNull(newNonce);
    }

    public ByteBuf getServerNonce() {
        return serverNonce;
    }

    public void setServerNonce(ByteBuf serverNonce) {
        this.serverNonce = Objects.requireNonNull(serverNonce);
    }

    public ByteBuf getAuthKey() {
        return authKey;
    }

    public void setAuthKey(ByteBuf authKey) {
        this.authKey = Objects.requireNonNull(authKey);
    }

    public long getServerSalt() {
        return serverSalt;
    }

    public void setServerSalt(long serverSalt) {
        this.serverSalt = serverSalt;
    }

    public ByteBuf getAuthAuxHash() {
        return authAuxHash;
    }

    public void setAuthAuxHash(ByteBuf authAuxHash) {
        this.authAuxHash = Objects.requireNonNull(authAuxHash);
    }

    public ServerDHParams getServerDHParams() {
        return serverDHParams;
    }

    public void setServerDHParams(ServerDHParams serverDHParams) {
        this.serverDHParams = Objects.requireNonNull(serverDHParams);
    }

    public AtomicInteger getRetry() {
        return retry;
    }

    public int getTimeOffset() {
        return timeOffset;
    }

    public void setTimeOffset(int serverTime) {
        int now = (int) (System.currentTimeMillis() / 1000);
        timeOffset = serverTime - now;
    }

    public void clear() {
        safeRelease(nonce);
        safeRelease(newNonce);
        safeRelease(serverNonce);
        safeRelease(authKey);
        safeRelease(authAuxHash);

        nonce = null;
        newNonce = null;
        serverNonce = null;
        authKey = null;
        serverSalt = 0;
        timeOffset = 0;
        authAuxHash = null;
        serverDHParams = null;
        retry.set(0);
    }
}
