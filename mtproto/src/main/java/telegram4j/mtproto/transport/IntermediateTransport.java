package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class IntermediateTransport implements Transport {
    public static final int ID = 0xeeeeeeee;

    private final AtomicInteger size = new AtomicInteger(-1);

    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(Integer.BYTES).writeIntLE(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload) {
        try {
            return payload.alloc().buffer(Integer.BYTES + payload.readableBytes())
                    .writeIntLE(payload.readableBytes())
                    .writeBytes(payload);
        } finally {
            ReferenceCountUtil.safeRelease(payload);
        }
    }

    @Override
    public ByteBuf decode(ByteBuf payload) {
        try {
            int length = payload.readIntLE();
            return payload.readBytes(length);
        } finally {
            size.set(-1);
        }
    }

    @Override
    public boolean canDecode(ByteBuf payload) {
        payload.markReaderIndex();
        try {
            int length = payload.readIntLE();
            return payload.readableBytes() == size.updateAndGet(i -> i == -1 ? length : i);
        } finally {
            payload.resetReaderIndex();
        }
    }
}
