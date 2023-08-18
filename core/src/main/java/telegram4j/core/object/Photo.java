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
package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.Context;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.file.ProfilePhotoContext;
import telegram4j.tl.BaseDocument;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.DocumentAttributeImageSize;
import telegram4j.tl.WebDocument;

import java.util.Objects;
import java.util.Optional;

/**
 * Representation for message and profile photos.
 *
 * <p> For separation between ordinal message photos and sent as document you
 * can check {@link FileReferenceId#getFileType()} of {@link #getFileReferenceId()}.
 */
public final class Photo extends Document {
    @Nullable
    private final DocumentAttributeImageSize sizeData;

    public Photo(MTProtoTelegramClient client, WebDocument data,
                 @Nullable String fileName, Context context,
                 DocumentAttributeImageSize sizeData) {
        super(client, data, fileName, context);
        this.sizeData = Objects.requireNonNull(sizeData);
    }

    public Photo(MTProtoTelegramClient client, BaseDocument data,
                 @Nullable String fileName, Context context,
                 DocumentAttributeImageSize sizeData) {
        super(client, data, fileName, context);
        this.sizeData = Objects.requireNonNull(sizeData);
    }

    public Photo(MTProtoTelegramClient client, BasePhoto data, Context context) {
        super(client, data, FileReferenceId.ofPhoto(data, context), null);

        sizeData = null;
    }

    public Photo(MTProtoTelegramClient client, BasePhoto data, ProfilePhotoContext context) {
        super(client, data, FileReferenceId.ofChatPhoto(data, context), null);

        sizeData = null;
    }

    /**
     * Gets original width of video document, if photo uploaded as document.
     *
     * @return The original width of video document, if photo uploaded as document
     */
    public Optional<Integer> getWidth() {
        return Optional.ofNullable(sizeData).map(DocumentAttributeImageSize::w);
    }

    /**
     * Gets original height of video document height, if photo uploaded as document
     *
     * @return The original height of video document height, if photo uploaded as document
     */
    public Optional<Integer> getHeight() {
        return Optional.ofNullable(sizeData).map(DocumentAttributeImageSize::h);
    }

    /**
     * Gets whether photo has mask stickers attached to it.
     *
     * @return {@code true} if photo has mask stickers attached to it.
     */
    public boolean hasStickers() {
        return data instanceof BasePhoto p && p.hasStickers();
    }

    @Override
    public String toString() {
        return "Photo{" +
                "data=" + data +
                ", fileReferenceId=" + fileReferenceId +
                '}';
    }
}
