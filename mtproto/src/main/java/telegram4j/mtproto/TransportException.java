package telegram4j.mtproto;

import java.io.Serial;

public final class TransportException extends MTProtoException {
    @Serial
    private static final long serialVersionUID = -25779734250996979L;

    private final int code;

    public TransportException(int code) {
        super("code: " + code);
        this.code = code;
    }

    public static boolean isError(int code) {
        return switch (code) {
            case -404, -444, -429 -> true;
            default -> false;
        };
    }

    public int getCode() {
        return code;
    }
}
