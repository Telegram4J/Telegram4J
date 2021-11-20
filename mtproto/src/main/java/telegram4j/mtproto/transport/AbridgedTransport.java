package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class AbridgedTransport implements Transport {
    public static final int ID = 0xef;

    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(Integer.BYTES).writeByte(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload) {
        int length = payload.readableBytes() / 4;
        ByteBuf buf = payload.alloc().buffer();
        if (length >= 0x7f) {
            buf.writeByte(0x7f);
            buf.writeMediumLE(length);
        } else {
            buf.writeByte(length);
        }
        buf.writeBytes(payload);
        return buf;
    }

    @Override
    public ByteBuf decode(ByteBuf payload) {
        int partialLength = payload.readUnsignedByte();
        if (partialLength == 0x7f) {
            partialLength = payload.readUnsignedMediumLE();
        }

        int payloadLength = partialLength * 4;
        return payload.readBytes(payloadLength);
    }
}
