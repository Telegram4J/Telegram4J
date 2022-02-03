package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.concurrent.atomic.AtomicInteger;

public class AbridgedTransport implements Transport {
    public static final int ID = 0xef;

    private final AtomicInteger size = new AtomicInteger(-1);
    private final AtomicInteger completed = new AtomicInteger(-1);

    @Override
    public ByteBuf identifier(ByteBufAllocator allocator) {
        return allocator.buffer(Integer.BYTES).writeByte(ID);
    }

    @Override
    public ByteBuf encode(ByteBuf payload, boolean quickAck) {
        try {
            int length = payload.readableBytes() / 4;
            ByteBuf buf = payload.alloc().buffer((length >= 0x7f ? 4 : 1) + payload.readableBytes());
            if (length >= 0x7f) {
                buf.writeByte(0x7f);
                buf.writeMediumLE(length);
            } else {
                buf.writeByte(length);
            }
            buf.writeBytes(payload);
            return buf;
        } finally {
            payload.release();
        }
    }

    @Override
    public ByteBuf decode(ByteBuf payload) {
        try {
            int partialLength = payload.readUnsignedByte();
            if (partialLength == 0x7f) {
                partialLength = payload.readUnsignedMediumLE();
            }

            int payloadLength = partialLength * 4;
            return payload.readBytes(payloadLength);
        } finally {
            payload.release();
            size.set(-1);
            completed.set(-1);
        }
    }

    @Override
    public boolean canDecode(ByteBuf payload) {
        int partialLength = payload.getByte(0);
        if (partialLength == 0x7f) {
            partialLength = payload.getUnsignedMediumLE(1);
        }

        int pad = partialLength == 0x7f ? 4 : 1;
        int length = partialLength * 4;
        int payloadLength = payload.readableBytes() - pad;
        if (length != payloadLength) {
            if (size.compareAndSet(-1, partialLength)) {
                completed.set(payloadLength);
                return false;
            }

            return completed.addAndGet(payloadLength + pad) == size.get();
        }
        return size.get() == -1;
    }

    @Override
    public boolean supportQuickAck() {
        return false;
    }

    @Override
    public void setQuickAckState(boolean enable) {
    }
}
