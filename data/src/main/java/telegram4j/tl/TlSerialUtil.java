package telegram4j.tl;

import io.netty.buffer.ByteBuf;

final class TlSerialUtil {

    private TlSerialUtil() {}

    static byte[] readBytes(ByteBuf buf, int length) {
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return bytes;
    }
}
