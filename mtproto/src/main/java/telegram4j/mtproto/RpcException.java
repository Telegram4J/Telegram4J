package telegram4j.mtproto;

import telegram4j.tl.api.TlMethod;
import telegram4j.tl.mtproto.RpcError;

public class RpcException extends MTProtoException {
    private static final long serialVersionUID = -4159674899075462143L;

    private final RpcError error;

    public RpcException(String message, RpcError error) {
        super(message);
        this.error = error;
    }

    static String prettyMethodName(TlMethod<?> method) {
        return method.getClass().getCanonicalName()
                .replace("telegram4j.tl.", "")
                .replace("request.", "")
                .replace("Immutable", "");
    }

    static RpcException create(RpcError error, DefaultMTProtoClient.PendingRequest request) {
        String orig = error.errorMessage();
        int argIdx = orig.indexOf("_X");
        String message = argIdx != -1 ? orig.substring(0, argIdx) : orig;
        String arg = argIdx != -1 ? orig.substring(argIdx) : null;

        String format = String.format("%s returned code: %d, message: %s%s",
                prettyMethodName(request.method), error.errorCode(),
                message, arg != null ? ", param: " + arg : "");

        return new RpcException(format, error);
    }

    public RpcError getError() {
        return error;
    }
}
