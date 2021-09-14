package telegram4j.tl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.json.api.tl.TlObject;
import telegram4j.tl.mtproto.TlSerializer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public final class TlSerialUtil {

    private static final ByteBuf EMPTY_BUFFER = new EmptyByteBuf(ByteBufAllocator.DEFAULT);

    public static final int VECTOR_ID = 0x1cb5c415;

    private TlSerialUtil() {
    }

    public static ByteBuf writeString(ByteBufAllocator allocator, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int capacity = (bytes.length <= 0xfd ? Integer.BYTES : Integer.BYTES * 4) + bytes.length;
        capacity += Math.ceil(capacity / 4f) * 4;

        ByteBuf buf = allocator.buffer(capacity);
        if (bytes.length <= 0xfd) {
            buf.writeIntLE(bytes.length);
        } else {
            buf.writeIntLE(0xfe);
            buf.writeIntLE(bytes.length & 0xff);
            buf.writeIntLE(bytes.length & 0xff00 >> 8);
            buf.writeIntLE(bytes.length & 0xff0000 >> 16);
        }

        buf.writeBytes(bytes);
        while (buf.writerIndex() % 4 != 0) {
            buf.writeByte(0);
        }
        return buf;
    }

    public static ByteBuf serializeLongVector(ByteBufAllocator allocator, List<Long> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (Long l : vector) {
            buf.writeLongLE(l);
        }
        return buf;
    }

    public static ByteBuf serializeIntVector(ByteBufAllocator allocator, List<Integer> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (Integer i : vector) {
            buf.writeIntLE(i);
        }
        return buf;
    }


    public static ByteBuf serializeByteVector(ByteBufAllocator allocator, List<byte[]> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (byte[] bytes : vector) {
            buf.writeBytes(bytes);
        }
        return buf;
    }

    public static ByteBuf serializeVector(ByteBufAllocator allocator, List<? extends TlObject> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (TlObject tlObject : vector) {
            buf.writeBytes(TlSerializer.serialize(allocator, tlObject));
        }
        return buf;
    }

    public static ByteBuf serializeFlags(ByteBufAllocator allocator, @Nullable Object value) {
        if (value == null) {
            return EMPTY_BUFFER;
        }

        if (value instanceof Byte) {
            return allocator.buffer(Byte.BYTES).writeByte((int) value);
        } else if (value instanceof Boolean) {
            return allocator.buffer(1).writeBoolean((boolean) value);
        } else if (value instanceof Integer) {
            return allocator.buffer(Integer.BYTES).writeIntLE((int) value);
        } else if (value instanceof Long) {
            return allocator.buffer(Long.BYTES).writeLongLE((long) value);
        } else if (value instanceof Double) {
            return allocator.buffer(Double.BYTES).writeDoubleLE((long) value);
        } else {
            return TlSerializer.serialize(allocator, (TlObject) value);
        }
    }

    public static int calculateFlags(Object... optionals) {
        int flags = 0;
        for (Object optional : optionals) {
            flags |= optional != null ? 1 : 0;
        }
        return flags;
    }

    public static byte[] readBytes(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }
}
