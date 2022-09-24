package telegram4j.mtproto.auth;

import telegram4j.mtproto.MTProtoException;

public class AuthorizationException extends MTProtoException {
    private static final long serialVersionUID = -3575489444121319286L;

    public AuthorizationException() {
    }

    public AuthorizationException(Throwable cause) {
        super(cause);
    }

    public AuthorizationException(String message) {
        super(message);
    }
}
