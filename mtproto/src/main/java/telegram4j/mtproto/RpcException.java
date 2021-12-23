package telegram4j.mtproto;

import telegram4j.tl.mtproto.RpcError;

public class RpcException extends RuntimeException {
    private static final long serialVersionUID = -4159674899075462143L;

    private final RpcError error;

    public RpcException(String message, RpcError error) {
        super(message);
        this.error = error;
    }

    static RpcException create(RpcError error, DefaultMTProtoClient.RequestTuple request) {
        String orig = error.errorMessage();
        int argIdx = orig.indexOf("_X");
        String message = argIdx != -1 ? orig.substring(0, argIdx) : orig;
        String arg = argIdx != -1 ? orig.substring(argIdx) : null;
        String methodDomainName = request.method.getClass().getPackageName()
                .replace("telegram4j.tl.request", "");
        String methodName = request.method.getClass().getSimpleName()
                .replace("Immutable", "");

        String format = String.format("%s.%s returned code: %d, message: %s%s",
                methodDomainName, methodName, error.errorCode(),
                message, arg != null ? ", param: " + arg : "");

        return new RpcException(format, error);
    }

    public RpcError getError() {
        return error;
    }
}
