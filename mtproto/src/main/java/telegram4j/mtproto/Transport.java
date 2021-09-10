package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public interface Transport {

    ByteBuf identifier(ByteBufAllocator allocator);

    ByteBuf encode(ByteBufAllocator allocator, ByteBuf payload);

    ByteBuf decode(ByteBufAllocator allocator, ByteBuf payload);
}
