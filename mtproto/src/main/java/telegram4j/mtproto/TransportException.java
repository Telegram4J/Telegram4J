package telegram4j.mtproto;

public class TransportException extends MTProtoException {
    private static final long serialVersionUID = -25779734250996979L;

    private final int code;

    public TransportException(int code) {
        super("code: " + code);
        this.code = code;
    }

    public static boolean isError(int code) {
        switch (code) {
            case -404:
            case -444:
            case -429: return true;
            default: return false;
        }
    }

    public int getCode() {
        return code;
    }
}
