package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import reactor.util.annotation.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

/** A MTProto transport which aligns data up to 4-byte. */
public class IntermediateTransport implements Transport {
    public static final int ID = 0xeeeeeeee;

    // size of current handling packet
    private final AtomicInteger size = new AtomicInteger(-1);
    private final AtomicInteger completed = new AtomicInteger(-1);

    private volatile boolean quickAck;

    public IntermediateTransport(boolean quickAck) {
        this.quickAck = quickAck;
    }

    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(Integer.BYTES).writeIntLE(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload, boolean quickAck) {
        int packetSize = payload.readableBytes();
        if (this.quickAck && quickAck) {
            packetSize |= QUICK_ACK_MASK;
        }

        return Unpooled.wrappedBuffer(payload.alloc().buffer(4).writeIntLE(packetSize), payload);
    }

    @Nullable
    @Override
    public ByteBuf decode(ByteBuf payload) {
        try {
            int length = payload.readIntLE();
            if (quickAck && (length & QUICK_ACK_MASK) != 0) {
                return payload.slice(0, 4);
            }

            if (!payload.isReadable(length)) {
                return null;
            }

            return payload.readSlice(length);
        } finally {
            size.set(-1);
            completed.set(-1);
        }
    }

    @Override
    public boolean canDecode(ByteBuf payload) {
        int length = payload.getIntLE(0);

        if (quickAck && size.get() == -1 && (length & QUICK_ACK_MASK) != 0) {
            return true;
        }

        int payloadLength = payload.readableBytes() - 4;
        if (length != payloadLength) { // is a part of stream
            if (size.compareAndSet(-1, length)) { // header of a stream
                completed.set(payloadLength);
                return false;
            }

            return completed.addAndGet(payloadLength + 4) == size.get();
        }
        return size.get() == -1;
    }

    @Override
    public boolean supportQuickAck() {
        return quickAck;
    }

    @Override
    public void setQuickAckState(boolean enable) {
        this.quickAck = enable;
    }
}
