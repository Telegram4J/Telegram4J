package telegram4j.mtproto;

import telegram4j.tl.mtproto.RpcError;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/** Subtype of {@link MTProtoException} which receives on RPC response. */
public class RpcException extends MTProtoException {
    private static final long serialVersionUID = 909166276209425603L;

    private final RpcError error;

    public RpcException(String message, RpcError error) {
        super(message);
        this.error = Objects.requireNonNull(error);
    }

    /**
     * Create {@link Predicate} for throwable which matches on
     * {@code RpcException} with one of specified error codes.
     *
     * @param codes The array of rpc method errors codes
     * @return A {@link Predicate} for throwable which matches
     * on {@code RpcException} with one of specified error codes.
     */
    public static Predicate<Throwable> isErrorCode(int... codes) {
        return t -> {
            if (t instanceof RpcException) {
                RpcException t0 = (RpcException) t;

                return IntStream.of(codes).anyMatch(c -> t0.error.errorCode() == c);
            }
            return false;
        };
    }

    /**
     * Create {@link Predicate} for throwable which matches on flood wait.
     *
     * @return A {@link Predicate} for throwable which matches flood wait errors.
     */
    public static Predicate<Throwable> isFloodWait() {
        return t -> {
            if (t instanceof RpcException) {
                RpcException t0 = (RpcException) t;

                return t0.error.errorCode() == 420 &&
                        t0.error.errorMessage().startsWith("FLOOD_WAIT_");
            }
            return false;
        };
    }

    /**
     * Gets original {@link RpcError} received as method response.
     *
     * @return The original {@link RpcError}.
     */
    public RpcError getError() {
        return error;
    }
}
