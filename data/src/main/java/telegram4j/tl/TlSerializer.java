package telegram4j.tl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import telegram4j.json.api.tl.TlObject;
import telegram4j.tl.mtproto.authkey.PqInnerData;
import telegram4j.tl.mtproto.authkey.ReqDhParams;

public final class TlSerializer {

    private TlSerializer() {}

    public static ByteBuf serializePqInnerData(ByteBufAllocator allocator, PqInnerData value) {
        return allocator.heapBuffer()
                .writeIntLE(value.identifier())
                .writeBytes(value.nonce())
                .writeBytes(value.serverNonce())
                .writeBytes(value.p())
                .writeBytes(value.q());
    }

    public static ByteBuf serializeReqDhParams(ByteBufAllocator allocator, ReqDhParams value) {
        return allocator.heapBuffer()
                .writeIntLE(value.identifier())
                .writeBytes(value.nonce())
                .writeBytes(value.serverNonce())
                .writeBytes(value.p())
                .writeBytes(value.q())
                .writeBytes(serialize(allocator, value.encryptedData()));
    }

    public static <T extends TlObject> ByteBuf serialize(ByteBufAllocator allocator, T value) {
        switch (value.identifier()) {
            case ReqDhParams.ID: return serializeReqDhParams(allocator, (ReqDhParams) value);
            case PqInnerData.ID: return serializePqInnerData(allocator, (PqInnerData) value);
            default: throw new IllegalStateException("Unexpected object id: " + value.identifier());
        }
    }
}
