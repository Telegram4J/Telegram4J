package telegram4j.mtproto;

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
