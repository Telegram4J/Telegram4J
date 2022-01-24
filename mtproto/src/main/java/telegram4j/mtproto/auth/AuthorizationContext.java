package telegram4j.mtproto.auth;

import telegram4j.tl.mtproto.ServerDHParams;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class AuthorizationContext {
    private volatile byte[] nonce;
    private volatile byte[] newNonce;
    private volatile byte[] serverNonce;
    private volatile byte[] authKey;
    private volatile long serverSalt;
    private volatile byte[] authAuxHash;
    private volatile ServerDHParams serverDHParams;
    private final AtomicInteger retry = new AtomicInteger();

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = Objects.requireNonNull(nonce, "nonce");
    }

    public byte[] getNewNonce() {
        return newNonce;
    }

    public void setNewNonce(byte[] newNonce) {
        this.newNonce = Objects.requireNonNull(newNonce, "newNonce");
    }

    public byte[] getServerNonce() {
        return serverNonce;
    }

    public void setServerNonce(byte[] serverNonce) {
        this.serverNonce = Objects.requireNonNull(serverNonce, "serverNonce");
    }

    public byte[] getAuthKey() {
        return authKey;
    }

    public void setAuthKey(byte[] authKey) {
        this.authKey = Objects.requireNonNull(authKey, "authKey");
    }

    public long getServerSalt() {
        return serverSalt;
    }

    public void setServerSalt(long serverSalt) {
        this.serverSalt = serverSalt;
    }

    public byte[] getAuthAuxHash() {
        return authAuxHash;
    }

    public void setAuthAuxHash(byte[] authAuxHash) {
        this.authAuxHash = Objects.requireNonNull(authAuxHash, "authAuxHash");
    }

    public ServerDHParams getServerDHParams() {
        return serverDHParams;
    }

    public void setServerDHParams(ServerDHParams serverDHParams) {
        this.serverDHParams = Objects.requireNonNull(serverDHParams, "serverDHParams");
    }

    public AtomicInteger getRetry() {
        return retry;
    }

    public void clear() {
        nonce = null;
        newNonce = null;
        serverNonce = null;
        authKey = null;
        serverSalt = 0;
        authAuxHash = null;
        serverDHParams = null;
        retry.set(0);
    }
}
