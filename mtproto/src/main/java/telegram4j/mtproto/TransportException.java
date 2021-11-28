package telegram4j.mtproto;

public class TransportException extends RuntimeException {

    private static final long serialVersionUID = -8781856208087639394L;

    private final int code;

    public TransportException(String message, int code) {
        super(message);
        this.code = code;
    }

    public static TransportException create(int code) {
        String message = null;

        switch (code) {
            case 404:
                message = "Attempt to invoke a non-existent object/method.";
                break;

            case 429:
                message = "Too many connections are established to the same IP in a too short lapse of time.";
                break;
        }

        String format = "code: " + code + ", message: " + message;
        return new TransportException(format, code);
    }

    public int getCode() {
        return code;
    }
}
