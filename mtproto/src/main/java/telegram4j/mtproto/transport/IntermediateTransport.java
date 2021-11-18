package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class IntermediateTransport implements Transport {
    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(Integer.BYTES).writeIntLE(0xeeeeeeee);
    }

    @Override
    public ByteBuf encode(ByteBufAllocator allocator, ByteBuf payload) {
        return allocator.buffer(Integer.BYTES + payload.readableBytes())
                .writeIntLE(payload.readableBytes())
                .writeBytes(payload);
    }

    @Override
    public ByteBuf decode(ByteBufAllocator allocator, ByteBuf payload) {
        int length = payload.readIntLE();
        return payload.readBytes(length);
    }
}
