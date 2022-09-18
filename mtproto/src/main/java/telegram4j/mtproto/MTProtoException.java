package telegram4j.mtproto;

/** General base class for all mtproto level problems. */
public class MTProtoException extends RuntimeException {

    public MTProtoException() {
    }

    public MTProtoException(Throwable cause) {
        super(cause);
    }

    public MTProtoException(String message) {
        super(message);
    }
}
