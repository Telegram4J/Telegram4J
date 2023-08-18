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

import io.netty.buffer.ByteBuf;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChatPhoto;
import telegram4j.tl.BaseUserProfilePhoto;
import telegram4j.tl.InputPeer;

import java.util.Objects;
import java.util.Optional;

/**
 * Low-quality chat profile photo.
 *
 * <p> There are 2 versions available for download: small ({@link ProfilePhoto#getSmallFileReferenceId()})
 * and big ({@link ProfilePhoto#getBigFileReferenceId()}).
 */
public final class ProfilePhoto implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final Variant2<BaseChatPhoto, BaseUserProfilePhoto> data;

    private final FileReferenceId smallFileReferenceId;
    private final FileReferenceId bigFileReferenceId;

    public ProfilePhoto(MTProtoTelegramClient client, BaseChatPhoto data, InputPeer peer) {
        this.client = Objects.requireNonNull(client);
        this.data = Variant2.ofT1(data);

        this.smallFileReferenceId = FileReferenceId.ofChatPhoto(data, false, peer);
        this.bigFileReferenceId = FileReferenceId.ofChatPhoto(data, true, peer);
    }

    public ProfilePhoto(MTProtoTelegramClient client, BaseUserProfilePhoto data, InputPeer peer) {
        this.client = Objects.requireNonNull(client);
        this.data = Variant2.ofT2(data);

        this.smallFileReferenceId = FileReferenceId.ofChatPhoto(data, false, peer);
        this.bigFileReferenceId = FileReferenceId.ofChatPhoto(data, true, peer);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets {@link FileReferenceId} of <b>small</b> chat photo.
     *
     * @return The {@link FileReferenceId} of <b>small</b> chat photo.
     */
    public FileReferenceId getSmallFileReferenceId() {
        return smallFileReferenceId;
    }

    /**
     * Gets serialized {@link FileReferenceId} of <b>big</b> chat photo.
     *
     * @return The serialized {@link FileReferenceId} of <b>big</b> chat photo.
     */
    public FileReferenceId getBigFileReferenceId() {
        return bigFileReferenceId;
    }

    /**
     * Gets whether peer has animated profile photo.
     *
     * @return {@code true} if peer has animated profile photo.
     */
    public boolean hasVideo() {
        return data.map(BaseChatPhoto::hasVideo, BaseUserProfilePhoto::hasVideo);
    }

    // TODO: docs
    public boolean isPersonal() {
        return data.map(e -> false, BaseUserProfilePhoto::personal);
    }

    /**
     * Gets id of chat photo.
     *
     * @return The id of chat photo.
     */
    public long getId() {
        return data.map(BaseChatPhoto::photoId, BaseUserProfilePhoto::photoId);
    }

    /**
     * Gets new {@link ByteBuf} with expanded stripped thumbnail for photo, if present.
     *
     * @return The new {@link ByteBuf} with expanded stripped thumbnail for photo, if present.
     */
    public Optional<ByteBuf> getThumb() {
        return Optional.ofNullable(data.map(BaseChatPhoto::strippedThumb, BaseUserProfilePhoto::strippedThumb))
                .map(TlEntityUtil::expandInlineThumb);
    }

    /**
     * Gets raw stripped thumbnail for photo, if present.
     *
     * @return The raw stripped thumbnail for photo, if present.
     */
    public Optional<ByteBuf> getStrippedThumb() {
        return Optional.ofNullable(data.map(BaseChatPhoto::strippedThumb, BaseUserProfilePhoto::strippedThumb));
    }

    /**
     * Gets id of DC that can be used for downloading this photo.
     *
     * @return The id of DC that can be used for downloading this photo.
     */
    public int getDcId() {
        return data.map(BaseChatPhoto::dcId, BaseUserProfilePhoto::dcId);
    }

    @Override
    public String toString() {
        return "ChatPhoto{" +
                "data=" + data +
                ", smallFileReferenceId=" + smallFileReferenceId +
                ", bigFileReferenceId=" + bigFileReferenceId +
                '}';
    }
}
