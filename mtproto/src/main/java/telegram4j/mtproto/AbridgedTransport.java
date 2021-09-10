package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class AbridgedTransport implements Transport {
    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(1).writeByte(0xef);
    }

    @Override
    public ByteBuf encode(ByteBufAllocator allocator, ByteBuf payload) {
        int length = payload.writerIndex() / 4;
        ByteBuf buf = allocator.buffer(payload.writerIndex() + (length >= 0xf ? 4 : 3));
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
        return null;
    }
}
