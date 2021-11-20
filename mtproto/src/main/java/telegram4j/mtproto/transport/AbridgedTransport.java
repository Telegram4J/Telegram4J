package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class AbridgedTransport implements Transport {
    public static final int ID = 0xef;

    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(1).writeByte(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload) {
        int length = payload.readableBytes() / 4;
        ByteBuf buf = payload.alloc().buffer(payload.readableBytes() + (length >= 0x7f ? 4 : 3));
        if (length >= 0x7f) {
            buf.writeByte(0x7f);
            buf.writeByte(length);
            buf.writeByte(length >> 8);
            buf.writeByte(length >> 16);
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
            partialLength = payload.readUnsignedByte() | payload.readUnsignedByte() << 8 | payload.readUnsignedByte() << 16;
        }

        int payloadLength = partialLength * 4;
        return payload.readBytes(payloadLength);
    }
}
