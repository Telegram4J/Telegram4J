package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public interface Transport {

    ByteBuf identifier(ByteBufAllocator allocator);

    ByteBuf encode(ByteBuf payload);

    ByteBuf decode(ByteBuf payload);
}
