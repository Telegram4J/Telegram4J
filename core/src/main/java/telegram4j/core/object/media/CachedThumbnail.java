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
package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;

import java.util.Objects;

public final class CachedThumbnail implements Thumbnail {

    private final telegram4j.tl.PhotoCachedSize data;

    public CachedThumbnail(telegram4j.tl.PhotoCachedSize data) {
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public char getType() {
        return data.type().charAt(0);
    }

    /**
     * Gets width of thumbnail.
     *
     * @return The width of thumbnail.
     */
    public int getWidth() {
        return data.w();
    }

    /**
     * Gets height of thumbnail.
     *
     * @return The height of thumbnail.
     */
    public int getHeight() {
        return data.h();
    }

    public ByteBuf getContent() {
        return data.bytes();
    }

    @Override
    public String toString() {
        return "PhotoCachedSize{" +
                "data=" + data +
                '}';
    }
}
