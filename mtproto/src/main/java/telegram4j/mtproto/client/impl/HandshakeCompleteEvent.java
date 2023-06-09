package telegram4j.mtproto.client.impl;

import telegram4j.mtproto.auth.AuthKey;

public record HandshakeCompleteEvent(AuthKey authKey, long serverSalt, int serverTimeDiff) {}
