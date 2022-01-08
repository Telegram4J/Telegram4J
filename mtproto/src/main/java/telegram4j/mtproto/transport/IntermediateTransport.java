package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.concurrent.atomic.AtomicInteger;

public class IntermediateTransport implements Transport {
    public static final int QUICK_ACK_MASK = 1 << 31;
    public static final int ID = 0xeeeeeeee;

    private final AtomicInteger size = new AtomicInteger(-1);
    private final AtomicInteger completed = new AtomicInteger(-1);

    private final boolean quickAck;

    public IntermediateTransport(boolean quickAck) {
        this.quickAck = quickAck;
    }

    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(Integer.BYTES).writeIntLE(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload) {
        try {
            int size = payload.readableBytes();
            int packetSize = size;
            if (quickAck) {
                packetSize |= QUICK_ACK_MASK;
            }

            return payload.alloc().buffer(Integer.BYTES + size)
                    .writeIntLE(packetSize)
                    .writeBytes(payload);
        } finally {
            payload.release();
        }
    }

    @Override
    public ByteBuf decode(ByteBuf payload) {
        try {
            int length = payload.readIntLE();
            if (quickAck && (length & QUICK_ACK_MASK) != 0) {
                return payload.retainedSlice(0, 4);
            }

            return payload.readBytes(length);
        } finally {
            payload.release();
            size.set(-1);
            completed.set(-1);
        }
    }

    @Override
    public boolean canDecode(ByteBuf payload) {
        payload.markReaderIndex();
        try {
            int length = payload.readIntLE();
            int payloadLength = payload.readableBytes();

            if (payloadLength < length) { // is a part of stream
                if (size.get() == -1) { // header of a stream
                    size.set(length);
                    completed.set(payloadLength);
                    return false;
                }

                // payload.readableBytes() + 4 need because reader index has already moved
                return completed.addAndGet(payloadLength + 4) == size.get();
            } else if (quickAck && payloadLength == 0 && (length & QUICK_ACK_MASK) != 0) {
                return true;
            }
            return true;
        } finally {
            payload.resetReaderIndex();
        }
    }

    @Override
    public boolean supportQuickAck() {
        return quickAck;
    }
}
