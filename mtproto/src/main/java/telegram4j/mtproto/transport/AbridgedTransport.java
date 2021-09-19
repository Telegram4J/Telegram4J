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
    public ByteBuf encode(ByteBufAllocator allocator, ByteBuf payload) {
        int length = payload.writerIndex() / 4;
        ByteBuf buf = allocator.buffer(payload.writerIndex() + (length >= 0x7f ? 4 : 3));
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
    public ByteBuf decode(ByteBufAllocator allocator, ByteBuf payload) {
        int partialLength = payload.readByte();
        if (partialLength == 0x7f) {
            partialLength = payload.readByte() + (payload.readByte() << 8) + (payload.readByte() << 16);
        }

        int payloadLength = partialLength * 4;
        return payload.readBytes(payloadLength);
    }
}
