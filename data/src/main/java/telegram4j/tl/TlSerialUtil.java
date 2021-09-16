package telegram4j.tl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import reactor.util.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TlSerialUtil {

    private static final ByteBuf EMPTY_BUFFER = new EmptyByteBuf(ByteBufAllocator.DEFAULT);

    public static final int VECTOR_ID = 0x1cb5c415;
    public static final int BOOL_TRUE_ID = 0x997275b5;
    public static final int BOOL_FALSE_ID = 0xbc799737;

    private TlSerialUtil() {
    }

    public static byte[] readBytes(ByteBuf buf) {
        int length = buf.readUnsignedByte();
        if (length == 0xfe) {
            length = buf.readByte() +
                    buf.readByte() << 8 +
                    buf.readByte() << 16;
        }

        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        while (buf.readerIndex() % 4 != 0) {
            buf.readByte();
        }

        return bytes;
    }

    public static String readString(ByteBuf buf) {
        return new String(readBytes(buf), StandardCharsets.UTF_8);
    }

    public static ByteBuf writeString(ByteBufAllocator allocator, String value) {
        return writeString(allocator, value.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteBuf writeString(ByteBufAllocator allocator, byte[] bytes) {
        int capacity = (bytes.length <= 0xfd ? Byte.BYTES : Byte.BYTES * 4) + bytes.length;
        if (capacity % 4 != 0) {
            capacity = (int) (Math.ceil(capacity / 4f) * 4);
        }

        ByteBuf buf = allocator.buffer(capacity);
        if (bytes.length <= 0xfd) {
            buf.writeByte(bytes.length);
        } else {
            buf.writeByte(0xfe);
            buf.writeByte(bytes.length & 0xff);
            buf.writeByte((bytes.length & 0xff00) >> 8);
            buf.writeByte((bytes.length & 0xff0000) >> 16);
        }

        buf.writeBytes(bytes);
        while (buf.writerIndex() % 4 != 0) {
            buf.writeByte(0);
        }
        return buf;
    }

    public static List<Long> deserializeLongVector(ByteBuf buf) {
        buf.readIntLE(); // vector id
        int size = buf.readIntLE();
        List<Long> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readLongLE());
        }
        return list;
    }

    public static List<Integer> deserializeIntVector(ByteBuf buf) {
        buf.readIntLE(); // vector id
        int size = buf.readIntLE();
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buf.readIntLE());
        }
        return list;
    }

    public static List<String> deserializeStringVector(ByteBuf buf) {
        buf.readIntLE(); // vector id
        int size = buf.readIntLE();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readString(buf));
        }
        return list;
    }

    public static List<byte[]> deserializeBytesVector(ByteBuf buf) {
        buf.readIntLE(); // vector id
        int size = buf.readIntLE();
        List<byte[]> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(readString(buf).getBytes(StandardCharsets.UTF_8));
        }
        return list;
    }

    public static <T extends TlSerializable> List<T> deserializeVector(ByteBuf buf) {
        buf.readIntLE(); // vector id
        int size = buf.readIntLE();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(TlDeserializer.deserialize(buf));
        }
        return list;
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

    public static ByteBuf serializeStringVector(ByteBufAllocator allocator, List<String> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (String o : vector) {
            buf.writeBytes(writeString(allocator, o));
        }
        return buf;
    }

    public static ByteBuf serializeBytesVector(ByteBufAllocator allocator, List<byte[]> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (byte[] bytes : vector) {
            buf.writeBytes(bytes);
        }
        return buf;
    }

    public static ByteBuf serializeVector(ByteBufAllocator allocator, List<? extends TlSerializable> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (TlSerializable o : vector) {
            buf.writeBytes(TlSerializer.serializeExact(allocator, o));
        }
        return buf;
    }

    public static ByteBuf serializeFlags(ByteBufAllocator allocator, @Nullable Object value) {
        if (value == null) {
            return EMPTY_BUFFER;
        }
        return serializeUnknown(allocator, value);
    }

    public static ByteBuf serializeUnknown(ByteBufAllocator allocator, Object value) {
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
        } else if (value instanceof TlTrue) {
            return allocator.buffer(1).writeByte(1);
        } else if (value instanceof byte[]) {
            byte[] value0 = (byte[]) value;
            return allocator.buffer(value0.length).writeBytes(value0);
        } else if (value instanceof List) {
            List<?> value0 = (List<?>) value;
            ByteBuf buf = allocator.buffer();
            buf.writeIntLE(VECTOR_ID);
            buf.writeIntLE(value0.size());
            for (Object o : value0) {
                buf.writeBytes(serializeUnknown(allocator, o));
            }
            return buf;
        } else if (value instanceof TlSerializable) {
            TlSerializable value0 = (TlSerializable) value;
            return TlSerializer.serializeExact(allocator, value0);
        } else {
            throw new IllegalArgumentException("Incorrect TL serializable type: " + value + " (" + value.getClass() + ")");
        }
    }
}
