package telegram4j.tl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import io.netty.buffer.*;
import reactor.core.Exceptions;
import reactor.util.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class TlSerialUtil {

    private static final ByteBuf EMPTY_BUFFER = new EmptyByteBuf(ByteBufAllocator.DEFAULT);

    public static final int VECTOR_ID = 0x1cb5c415;
    public static final int BOOL_TRUE_ID = 0x997275b5;
    public static final int BOOL_FALSE_ID = 0xbc799737;
    public static final int JSON_NULL_ID = 0x3f6d7b68;
    public static final int JSON_STRING_ID = 0xb71e767a;
    public static final int JSON_NUMBER_ID = 0x2be0dfa4;
    public static final int JSON_BOOLEAN_ID = 0xc7345e6a;
    public static final int JSON_ARRAY_ID = 0xf7444763;
    public static final int JSON_OBJECT_ID = 0x99c1d49d;
    public static final int JSON_ENTRY_ID = 0xc0de1bd9;

    private TlSerialUtil() {
    }

    public static ByteBuf compressGzip(ByteBufAllocator allocator, TlObject object) {
        ByteBufOutputStream bufOut = new ByteBufOutputStream(allocator.buffer());
        try (GZIPOutputStream out = new GZIPOutputStream(bufOut)) {
            ByteBuf buf = TlSerializer.serialize(allocator, object);
            out.write(ByteBufUtil.getBytes(buf));
            return bufOut.buffer();
        }catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

    public static <T extends TlObject> T decompressGzip(ByteBuf packed) {
        try (GZIPInputStream in = new GZIPInputStream(new ByteBufInputStream(packed))) {
            ByteBuf result = packed.alloc().buffer();
            int remaining = Integer.MAX_VALUE;
            int n;
            do {
                byte[] buf1 = new byte[Math.min(remaining, 1024 * 8)];
                int nread = 0;

                while ((n = in.read(buf1, nread, Math.min(buf1.length - nread, remaining))) > 0) {
                    nread += n;
                    remaining -= n;
                }

                if (nread > 0) {
                    result.writeBytes(buf1, 0, nread);
                }
            } while (n >= 0 && remaining > 0);

            return TlDeserializer.deserialize(result);
        }catch (IOException e) {
            throw Exceptions.propagate(e);
        }
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

    public static byte[] deserializeBytes(ByteBuf buf) {
        int count = buf.readUnsignedByte();
        int start = 1;
        if (count >= 0xfe) {
            count = buf.readUnsignedMediumLE();
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

    public static String deserializeString(ByteBuf buf) {
        return new String(deserializeBytes(buf), StandardCharsets.UTF_8);
    }

    public static ByteBuf serializeString(ByteBufAllocator allocator, String value) {
        return serializeBytes(allocator, value.getBytes(StandardCharsets.UTF_8));
    }

    public static ByteBuf serializeBytes(ByteBufAllocator allocator, byte[] bytes) {
        ByteBuf buf = allocator.buffer();

        if (bytes.length >= 0xfe) {
            buf.writeByte(0xfe);
            buf.writeMediumLE(bytes.length);
        } else {
            buf.writeByte(bytes.length);
        }

        buf.writeBytes(bytes);

        int offset = ((bytes.length >= 0xfe ? 4 : 1) + bytes.length) % 4;
        if (offset != 0) {
            byte[] data = new byte[4 - offset];
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
        return deserializeVector0(buf, false, TlSerialUtil::deserializeString);
    }

    public static List<byte[]> deserializeBytesVector(ByteBuf buf) {
        return deserializeVector0(buf, false, TlSerialUtil::deserializeBytes);
    }

    public static <T> List<T> deserializeVector0(ByteBuf buf, boolean bare, Function<? super ByteBuf, ? extends T> parser) {
        int vectorId;
        if (!bare && (vectorId = buf.readIntLE()) != VECTOR_ID) {
            throw new IllegalStateException("Incorrect vector identifier: " + vectorId);
        }
        int size = buf.readIntLE();
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(parser.apply(buf));
        }
        return list;
    }

    public static <T> List<T> deserializeVector(ByteBuf buf, boolean bare) {
        return deserializeVector0(buf, bare, TlDeserializer::deserialize);
    }

    public static <T> List<T> deserializeVector(ByteBuf buf) {
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
            buf.writeBytes(serializeString(allocator, o));
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

    public static ByteBuf serializeVector(ByteBufAllocator allocator, List<? extends TlObject> vector) {
        ByteBuf buf = allocator.buffer();
        buf.writeIntLE(VECTOR_ID);
        buf.writeIntLE(vector.size());
        for (TlObject o : vector) {
            buf.writeBytes(TlSerializer.serialize(allocator, o));
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
        } else if (value instanceof Integer) {
            return allocator.buffer(Integer.BYTES).writeIntLE((int) value);
        } else if (value instanceof Long) {
            return allocator.buffer(Long.BYTES).writeLongLE((long) value);
        } else if (value instanceof Boolean) {
            return allocator.buffer(Integer.BYTES).writeIntLE((boolean) value ? BOOL_TRUE_ID : BOOL_FALSE_ID);
        } else if (value instanceof Double) {
            return allocator.buffer(Double.BYTES).writeDoubleLE((long) value);
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
        } else if (value instanceof TlObject) {
            TlObject value0 = (TlObject) value;
            return TlSerializer.serialize(allocator, value0);
        } else if (value instanceof JsonNode) {
            JsonNode value0 = (JsonNode) value;
            return serializeJsonNode(allocator, value0);
        } else {
            throw new IllegalArgumentException("Incorrect TL serializable type: " + value + " (" + value.getClass() + ")");
        }
    }

    public static ByteBuf serializeJsonNode(ByteBufAllocator allocator, JsonNode node) {
        ByteBuf buf = allocator.buffer();
        switch (node.getNodeType()) {
            case NULL: return buf.writeIntLE(JSON_NULL_ID);
            case STRING: return buf.writeIntLE(JSON_STRING_ID)
                    .writeBytes(serializeString(allocator, node.asText()));
            case NUMBER: return buf.writeIntLE(JSON_NUMBER_ID)
                    .writeDoubleLE(node.asDouble());
            case BOOLEAN: return buf.writeIntLE(JSON_BOOLEAN_ID)
                    .writeIntLE(node.asBoolean() ? BOOL_TRUE_ID : BOOL_FALSE_ID);
            case ARRAY:
                buf.writeIntLE(JSON_ARRAY_ID);
                buf.writeIntLE(VECTOR_ID);
                buf.writeIntLE(node.size());
                node.elements().forEachRemaining(n -> buf.writeBytes(serializeJsonNode(allocator, n)));
                return buf;
            case OBJECT:
                buf.writeIntLE(JSON_OBJECT_ID);
                buf.writeIntLE(VECTOR_ID);
                buf.writeIntLE(node.size());
                node.fields().forEachRemaining(f -> buf.writeIntLE(JSON_ENTRY_ID)
                        .writeBytes(serializeString(allocator, f.getKey()))
                        .writeBytes(serializeJsonNode(allocator, f.getValue())));
                return buf;
            default: throw new IllegalStateException("Incorrect json node type: " + node.getNodeType());
        }
    }

    public static JsonNode deserializeJsonNode(ByteBuf buf) {
        int identifier = buf.readIntLE();
        switch (identifier) {
            case JSON_NULL_ID: return NullNode.instance;
            case JSON_BOOLEAN_ID: return BooleanNode.valueOf(buf.readIntLE() == BOOL_TRUE_ID);
            case JSON_STRING_ID: return TextNode.valueOf(deserializeString(buf));
            case JSON_NUMBER_ID: return DoubleNode.valueOf(buf.readDoubleLE());
            case JSON_ARRAY_ID:
                int vectorId;
                if ((vectorId = buf.readIntLE()) != VECTOR_ID) {
                    throw new IllegalStateException("Incorrect vector identifier: " + vectorId);
                }
                int size = buf.readIntLE();
                ArrayNode node = JsonNodeFactory.instance.arrayNode(size);
                for (int i = 0; i < size; i++) {
                    node.add(deserializeJsonNode(buf));
                }
                return node;
            case JSON_OBJECT_ID:
                int vectorId0;
                if ((vectorId0 = buf.readIntLE()) != VECTOR_ID) {
                    throw new IllegalStateException("Incorrect vector identifier: " + vectorId0);
                }
                int size0 = buf.readIntLE();
                ObjectNode node0 = JsonNodeFactory.instance.objectNode();
                for (int i = 0; i < size0; i++) {
                    String name = deserializeString(buf);
                    node0.set(name, deserializeJsonNode(buf));
                }
                return node0;
            default: throw new IllegalArgumentException("Incorrect json node identifier: 0x" + Integer.toHexString(identifier));
        }
    }
}
