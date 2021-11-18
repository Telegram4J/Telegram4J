package telegram4j.tl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import reactor.util.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class TlSerialUtil {

    private static final ByteBuf EMPTY_BUFFER = new EmptyByteBuf(ByteBufAllocator.DEFAULT);

    public static final int VECTOR_ID = 0x1cb5c415;
    public static final int BOOL_TRUE_ID = 0x997275b5;
    public static final int BOOL_FALSE_ID = 0xbc799737;

    private TlSerialUtil() {
    }

    public static byte[] readInt128(ByteBuf buf) {
        return readBytes(buf, Long.BYTES * 2);
    }

    public static byte[] readInt256(ByteBuf buf) {
        return readBytes(buf, Long.BYTES * 4);
    }

    public static byte[] readBytes(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }

    public static byte[] readBytes(ByteBuf buf) {
        int count = buf.readUnsignedByte();
        int start = 1;
        if (count >= 0xfe) {
            count = buf.readUnsignedByte() | buf.readUnsignedByte() << 8 | buf.readUnsignedByte() << 16;
            start = 4;
        }

        byte[] bytes = readBytes(buf, count);
        int offset = (count + start) % 4;
        if (offset != 0) {
            int offsetCount = 4 - offset;
            buf.skipBytes(offsetCount);
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
        ByteBuf buf = allocator.buffer();

        int startOffset = 1;
        if (bytes.length >= 0xfe) {
            startOffset = 4;
            buf.writeByte(0xfe);
            buf.writeByte(bytes.length & 0xff);
            buf.writeByte(bytes.length >> 8 & 0xff);
            buf.writeByte(bytes.length >> 16 & 0xff);
        } else {
            buf.writeByte(bytes.length);
        }

        buf.writeBytes(bytes);

        int offset = (bytes.length + startOffset) % 4;
        if (offset != 0) {
            int offsetCount = 4 - offset;
            byte[] data = new byte[offsetCount];
            buf.writeBytes(data);
        }

        return buf;
    }

    public static List<Long> deserializeLongVector(ByteBuf buf) {
        return deserializeVector0(buf, false, ByteBuf::readLongLE);
    }

    public static List<Integer> deserializeIntVector(ByteBuf buf) {
        return deserializeVector0(buf, false, ByteBuf::readIntLE);
    }

    public static List<String> deserializeStringVector(ByteBuf buf) {
        return deserializeVector0(buf, false, TlSerialUtil::readString);
    }

    public static List<byte[]> deserializeBytesVector(ByteBuf buf) {
        return deserializeVector0(buf, false, TlSerialUtil::readBytes);
    }

    public static <T> List<T> deserializeVector0(ByteBuf buf, boolean bare, Function<? super ByteBuf, ? extends T> parser) {
        assert bare || buf.readIntLE() == VECTOR_ID;
        int size = buf.readIntLE();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(parser.apply(buf));
        }
        return list;
    }

    public static <T extends TlSerializable> List<T> deserializeVector(ByteBuf buf, boolean bare) {
        return deserializeVector0(buf, bare, TlDeserializer::deserialize);
    }

    public static <T extends TlSerializable> List<T> deserializeVector(ByteBuf buf) {
        return deserializeVector(buf, false);
    }

    public static ByteBuf serializeLongVector(ByteBufAllocator allocator, List<Long> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (long l : vector) {
            buf.writeLongLE(l);
        }
        return buf;
    }

    public static ByteBuf serializeIntVector(ByteBufAllocator allocator, List<Integer> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (int i : vector) {
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
