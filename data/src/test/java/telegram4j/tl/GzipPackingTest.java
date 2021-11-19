package telegram4j.tl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import org.junit.jupiter.api.Test;
import telegram4j.tl.mtproto.GzipPacked;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GzipPackingTest {

    static ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;

    @Test
    void chat() {
        Chat chat = ChatEmpty.builder()
                .id(1337)
                .build();

        GzipPacked pack = GzipPacked.builder()
                .packedData(ByteBufUtil.getBytes(TlSerialUtil.compressGzip(alloc, chat)))
                .build();

        ByteBuf serialized = TlSerializer.serialize(alloc, pack);
        GzipPacked packDeserialized = TlDeserializer.deserialize(serialized);
        Chat deserializedChat = TlSerialUtil.decompressGzip(alloc.buffer().writeBytes(packDeserialized.packedData()));
        assertEquals(chat, deserializedChat);
    }
}
