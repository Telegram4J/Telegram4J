package telegram4j.mtproto;

public class TransportException extends RuntimeException {

    private static final long serialVersionUID = -8781856208087639394L;

    private final int code;

    public TransportException(String message, int code) {
        super(message);
        this.code = code;
    }

    static TransportException create(int code) {
        code = Math.abs(code);
        String message = null;

        switch (code) {
            case 404:
                message = "Attempt to invoke a non-existent object/method";
                break;
        }

        message = "code: " + code + ", " + message;

        return new TransportException(message, code);
    }

    public int getCode() {
        return code;
    }
}
