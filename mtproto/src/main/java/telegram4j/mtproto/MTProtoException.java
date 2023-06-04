package telegram4j.mtproto;

import telegram4j.mtproto.auth.AuthorizationException;

import java.io.Serial;

/** General base class for all mtproto level problems. */
public sealed class MTProtoException extends RuntimeException
        permits RpcException, TransportException, AuthorizationException {

    @Serial
    private static final long serialVersionUID = 2419676857037952979L;

    public MTProtoException() {
    }

    public MTProtoException(Throwable cause) {
        super(cause);
    }

    public MTProtoException(String message) {
        super(message);
    }
}
