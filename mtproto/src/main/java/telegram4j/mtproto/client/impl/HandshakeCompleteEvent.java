package telegram4j.mtproto.client.impl;

import telegram4j.mtproto.auth.AuthKey;

import java.util.Objects;

public record HandshakeCompleteEvent(AuthKey authKey, long serverSalt, int serverTimeDiff) {
    public HandshakeCompleteEvent {
        Objects.requireNonNull(authKey);
    }
}
