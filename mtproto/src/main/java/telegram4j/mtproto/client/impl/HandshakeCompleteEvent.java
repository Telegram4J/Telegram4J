package telegram4j.mtproto.client.impl;

import telegram4j.mtproto.auth.AuthKey;

record HandshakeCompleteEvent(AuthKey authKey, long serverSalt, int serverTimeDiff) {}
