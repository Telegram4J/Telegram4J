package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import reactor.util.annotation.Nullable;

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
        int length = payload.readableBytes() / 4;
        ByteBuf header = payload.alloc().buffer((length >= 0x7f ? 4 : 1) + payload.readableBytes());
        if (length >= 0x7f) {
            header.writeByte(0x7f);
            header.writeMediumLE(length);
        } else {
            header.writeByte(length);
        }
        return Unpooled.wrappedBuffer(header, payload);
    }

    @Nullable
    @Override
    public ByteBuf decode(ByteBuf payload) {
        try {
            int partialLength = payload.readUnsignedByte();
            if (partialLength == 0x7f) {
                partialLength = payload.readUnsignedMediumLE();
            }

            int payloadLength = partialLength * 4;
            if (!payload.isReadable(payloadLength)) {
                return null;
            }

            return payload.readSlice(payloadLength);
        } finally {
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
