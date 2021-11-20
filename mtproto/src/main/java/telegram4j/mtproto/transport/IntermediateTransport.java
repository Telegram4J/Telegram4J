package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class IntermediateTransport implements Transport {
    public static final int ID = 0xeeeeeeee;

    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(Integer.BYTES).writeIntLE(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload) {
        return payload.alloc().buffer(Integer.BYTES + payload.readableBytes())
                .writeIntLE(payload.readableBytes())
                .writeBytes(payload);
    }

    @Override
    public ByteBuf decode(ByteBuf payload) {
        int length = payload.readIntLE();
        return payload.readBytes(length);
    }
}
