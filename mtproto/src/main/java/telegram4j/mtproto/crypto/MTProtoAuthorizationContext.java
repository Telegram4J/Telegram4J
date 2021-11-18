package telegram4j.mtproto.crypto;

import telegram4j.tl.mtproto.ServerDHParams;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class MTProtoAuthorizationContext {
    private byte[] nonce;
    private byte[] newNonce;
    private byte[] serverNonce;
    private byte[] authKey;
    private byte[] serverSalt;
    private byte[] authAuxHash;
    private ServerDHParams serverDHParams;
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

    public byte[] getServerSalt() {
        return serverSalt;
    }

    public void setServerSalt(byte[] serverSalt) {
        this.serverSalt = Objects.requireNonNull(serverSalt, "serverSalt");
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
}
