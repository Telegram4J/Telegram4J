package telegram4j.mtproto.auth;

import telegram4j.mtproto.MTProtoException;

public class AuthorizationException extends MTProtoException {

    public AuthorizationException() {
    }

    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    public AuthorizationException(String message) {
        super(message);
    }
}
