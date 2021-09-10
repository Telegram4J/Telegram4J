package telegram4j.json.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.immutables.value.Value;

@Value.Immutable
public interface ReqDhParams extends TlObject<ReqDhParams> {

    static ImmutableReqDhParams.Builder builder() {
        return ImmutableReqDhParams.builder();
    }

    int ID = 0xd712e4be;

    byte[] nonce();

    byte[] serverNonce();

    byte[] p();

    byte[] q();

    PqInnerData encryptedData();

    @Override
    default int getId() {
        return ID;
    }

    @Override
    default ByteBuf serialize(ByteBufAllocator allocator) {
        return allocator.heapBuffer()
                .writeIntLE(getId())
                .writeBytes(nonce())
                .writeBytes(serverNonce())
                .writeBytes(p())
                .writeBytes(q())
                .writeBytes(encryptedData().serialize(allocator));
    }

    @Override
    default ReqDhParams deserialize(ByteBuf buf) {
        return builder()
                .id(buf.readIntLE())
                .nonce(TlSerialUtil.readBytes(buf, 16))
                .serverNonce(TlSerialUtil.readBytes(buf, 16))
                .p(TlSerialUtil.readBytes(buf, 8))
                .q(TlSerialUtil.readBytes(buf, 8))
                .build();
    }
}
