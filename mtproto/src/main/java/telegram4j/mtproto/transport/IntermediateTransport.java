package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

/** A MTProto transport which aligns data up to 4-byte. */
public class IntermediateTransport implements Transport {
    public static final int ID = 0xeeeeeeee;

    private final boolean useQuickAck;

    public IntermediateTransport(boolean useQuickAck) {
        this.useQuickAck = useQuickAck;
    }

    @Override
    public ByteBuf identifier(ByteBufAllocator alloc) {
        return alloc.buffer(Integer.BYTES).writeIntLE(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload, boolean quickAck) {
        int packetSize = payload.readableBytes();
        if (quickAck && useQuickAck) {
            packetSize |= QUICK_ACK_MASK;
        }

        return Unpooled.wrappedBuffer(payload.alloc().buffer(4).writeIntLE(packetSize), payload);
    }

    @Override
    public boolean supportsQuickAck() {
        return useQuickAck;
    }

    @Override
    public ByteBuf tryDecode(ByteBuf payload) {
        if (!payload.isReadable(4)) {
            return null;
        }

        payload.markReaderIndex();
        int length = payload.readIntLE();

        if ((length & QUICK_ACK_MASK) != 0 && useQuickAck) {
            payload.resetReaderIndex();
            return payload.readRetainedSlice(4);
        }

        if (payload.isReadable(length)) {
            return payload.readRetainedSlice(length);
        }

        payload.resetReaderIndex();
        return null;
    }
}
