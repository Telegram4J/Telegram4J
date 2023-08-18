/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputStickerSet;
import telegram4j.tl.TlSerializer;

public final class StickerSetContext extends Context {
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
