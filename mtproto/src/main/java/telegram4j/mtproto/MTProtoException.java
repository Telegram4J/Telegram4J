package telegram4j.mtproto;

/** General base class for all mtproto level problems. */
public class MTProtoException extends RuntimeException {
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
