package telegram4j.tl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatTest {

    @Test
    void chat() {
        Chat expected = BaseChat.builder()
                .version(1)
                .date(1337)
                .title("A!")
                .id(10)
                .photo(ChatPhotoEmpty.instance())
                .participantsCount(99)
                .left(TlTrue.INSTANCE)
                .callNotEmpty(TlTrue.INSTANCE)
                .build();

        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        ByteBuf bytes = TlSerializer.serializeExact(allocator, expected);

        Chat result = TlDeserializer.deserialize(bytes);
        assertEquals(result, expected);
    }
}
