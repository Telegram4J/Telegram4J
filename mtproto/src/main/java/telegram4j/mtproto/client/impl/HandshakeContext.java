package telegram4j.mtproto.client.impl;

import io.netty.buffer.ByteBuf;
import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.auth.DhPrimeChecker;
import telegram4j.tl.mtproto.ServerDHParams;

import java.util.Objects;

public final class HandshakeContext {

    private final DhPrimeChecker dhPrimeChecker;
    private final PublicRsaKeyRegister publicRsaKeyRegister;

    private ByteBuf nonce;
    private ByteBuf newNonce;
    private ByteBuf serverNonce;
    private ByteBuf authKey;
    private long serverSalt;
    private ByteBuf authKeyHash;
    private ServerDHParams serverDHParams;
    private int serverTimeDiff;
    private int retry;

    public HandshakeContext(DhPrimeChecker dhPrimeChecker, PublicRsaKeyRegister publicRsaKeyRegister) {
        this.dhPrimeChecker = dhPrimeChecker;
        this.publicRsaKeyRegister = publicRsaKeyRegister;
    }

    public DhPrimeChecker dhPrimeChecker() {
        return dhPrimeChecker;
    }

    public PublicRsaKeyRegister publicRsaKeyRegister() {
        return publicRsaKeyRegister;
    }

    public ByteBuf nonce() {
        return nonce;
    }

    public void nonce(ByteBuf nonce) {
        this.nonce = Objects.requireNonNull(nonce);
    }

    public ByteBuf newNonce() {
        return newNonce;
    }

    public void newNonce(ByteBuf newNonce) {
        this.newNonce = Objects.requireNonNull(newNonce);
    }

    public ByteBuf serverNonce() {
        return serverNonce;
    }

    public void serverNonce(ByteBuf serverNonce) {
        this.serverNonce = Objects.requireNonNull(serverNonce);
    }

    public void authKey(ByteBuf authKey) {
        this.authKey = Objects.requireNonNull(authKey);
    }

    public ByteBuf authKey() {
        return authKey;
    }

    public void serverSalt(long serverSalt) {
        this.serverSalt = serverSalt;
    }

    public long serverSalt() {
        return serverSalt;
    }

    public void authKeyHash(ByteBuf authKeyHash) {
        this.authKeyHash = Objects.requireNonNull(authKeyHash);
    }

    public ByteBuf authKeyHash() {
        return authKeyHash;
    }

    public ServerDHParams serverDHParams() {
        return serverDHParams;
    }

    public void serverDHParams(ServerDHParams serverDHParams) {
        this.serverDHParams = serverDHParams;
    }

    public void serverTimeDiff(int serverTimeDiff) {
        this.serverTimeDiff = serverTimeDiff;
    }

    public int serverTimeDiff() {
        return serverTimeDiff;
    }

    public int retry() {
        return retry;
    }

    public long getRetryAndIncrement() {
        return retry++;
    }
}
