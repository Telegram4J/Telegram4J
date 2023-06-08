package telegram4j.mtproto;

import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.api.TlMethod;

public final class DiscardedRpcRequestException extends MTProtoException {
    private final TlMethod<?> method;

    public DiscardedRpcRequestException(TlMethod<?> method) {
        super("Request discarded due to backpressure: " + TlEntityUtil.schemaTypeName(method));
        this.method = method;
    }

    public TlMethod<?> getMethod() {
        return method;
    }
}
