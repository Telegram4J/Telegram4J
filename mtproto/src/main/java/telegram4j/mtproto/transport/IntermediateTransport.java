package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import reactor.util.Logger;
import reactor.util.Loggers;

/** A MTProto transport which aligns data up to 4-byte. */
public class IntermediateTransport implements Transport {
    private static final Logger log = Loggers.getLogger(IntermediateTransport.class);

    public static final int ID = 0xeeeeeeee;

    private int size = -1;

    private volatile boolean quickAck;

    public IntermediateTransport(boolean quickAck) {
        this.quickAck = quickAck;
    }

    @Override
    public ByteBuf identifier(ByteBufAllocator alloc) {
        return alloc.buffer(Integer.BYTES).writeIntLE(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload, boolean quickAck) {
        int packetSize = payload.readableBytes();
        if (quickAck && this.quickAck) {
            packetSize |= QUICK_ACK_MASK;
        }

        return Unpooled.wrappedBuffer(payload.alloc().buffer(4).writeIntLE(packetSize), payload);
    }

    @Override
    public boolean supportQuickAck() {
        return quickAck;
    }

    @Override
    public void setQuickAckState(boolean enable) {
        this.quickAck = enable;
    }

    @Override
    public ByteBuf decode(ByteBuf payload) {
        if (!payload.isReadable(4)) {
            return null;
        }

        payload.markReaderIndex();
        int length = payload.readIntLE();

        if ((length & QUICK_ACK_MASK) != 0 && quickAck) {
            payload.resetReaderIndex();

            return payload.readRetainedSlice(4);
        }

        int payloadLength = payload.readableBytes();
        if (length != payloadLength) { // is a part of stream
            if (size == -1) { // header of a stream
                // log.debug("[header] {} / {}", payloadLength, length);

                if (payloadLength >= length) {
                    ByteBuf buf = payload.readRetainedSlice(length);
                    size = -1;
                    return buf;
                }

                size = length;
                payload.resetReaderIndex();
                return null;
            } // case for big streams (untested)

            payloadLength += 4; // return 4 bytes from length

            // log.debug("[stream] {} / {}", payloadLength, size);
            if (payloadLength >= size) { // completed
                ByteBuf buf = payload.readRetainedSlice(size);
                size = -1;
                return buf;
            } else {
                payload.resetReaderIndex();
                return null;
            }
        }

        // if (size != -1) {
        //     log.debug("[stream] {}", payloadLength);
        // } else {
        //     log.debug("[packet] r: {}", payloadLength);
        // }

        // packet is not slice
        size = -1;
        return payload.readRetainedSlice(length);
    }
}
