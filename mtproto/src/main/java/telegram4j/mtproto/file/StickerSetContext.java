package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputStickerSet;
import telegram4j.tl.TlSerializer;

public class StickerSetContext extends Context {
    private final InputStickerSet stickerSet;

    StickerSetContext(InputStickerSet stickerSet) {
        this.stickerSet = stickerSet;
    }

    @Override
    public Type getType() {
        return Type.STICKER_SET;
    }

    public InputStickerSet getStickerSet() {
        return stickerSet;
    }

    @Override
    void serialize(ByteBuf buf) {
        TlSerializer.serialize(buf, stickerSet);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickerSetContext that = (StickerSetContext) o;
        return stickerSet.equals(that.stickerSet);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + stickerSet.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "StickerSetContext{" +
                "stickerSet=" + stickerSet +
                '}';
    }
}
