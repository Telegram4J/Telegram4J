package telegram4j.json.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public interface TlSerializable<T extends TlSerializable<T>> {

    ByteBuf serialize(ByteBufAllocator allocator);

    T deserialize(ByteBuf buf);
}
