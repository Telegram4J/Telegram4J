package telegram4j.tl;

import io.netty.buffer.ByteBuf;
import telegram4j.tl.mtproto.TlObject;
import telegram4j.tl.mtproto.authkey.PqInnerData;
import telegram4j.tl.mtproto.authkey.ReqDhParams;

import static telegram4j.tl.TlSerialUtil.*;

public final class TlDeserializer {

    private TlDeserializer() {}

    public static PqInnerData deserializePqInnerData(ByteBuf buf) {
        return PqInnerData.builder()
                .nonce(readBytes(buf, 16))
                .serverNonce(readBytes(buf, 16))
                .p(readBytes(buf, 8))
                .q(readBytes(buf, 8))
                .build();
    }

    public static ReqDhParams deserializeReqDhParams(ByteBuf buf) {
        return ReqDhParams.builder()
                .nonce(readBytes(buf, 16))
                .serverNonce(readBytes(buf, 16))
                .p(readBytes(buf, 8))
                .q(readBytes(buf, 8))
                .build();
    }

    @SuppressWarnings("unchecked")
    public static <T extends TlObject> T deserialize(ByteBuf buf, int id) {
        switch (id) {
            case ReqDhParams.ID: return (T) deserializeReqDhParams(buf);
            case PqInnerData.ID: return (T) deserializePqInnerData(buf);
            default: throw new IllegalStateException("Unexpected object id: " + id);
        }
    }
}
