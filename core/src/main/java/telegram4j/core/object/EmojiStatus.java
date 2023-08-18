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

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Representation of premium user status with custom emoji. */
public final class EmojiStatus implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final long emojiId;
    @Nullable
    private final Instant untilTimestamp;

    public EmojiStatus(MTProtoTelegramClient client, long emojiId, @Nullable Instant untilTimestamp) {
        this.client = Objects.requireNonNull(client);
        this.emojiId = emojiId;
        this.untilTimestamp = untilTimestamp;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Requests to retrieve {@link Sticker custom emoji} represented by this status.
     *
     * @return A {@link Mono} emitting on successful completion {@link Sticker custom emoji}.
     */
    public Mono<Sticker> getCustomEmoji() {
        return client.getCustomEmoji(emojiId);
    }

    /**
     * Gets id of {@link Sticker custom emoji}.
     *
     * @return id of {@link Sticker custom emoji}.
     */
    public long getEmojiId() {
        return emojiId;
    }

    /**
     * Requests to retrieve {@link Sticker custom emoji} by {@link #getEmojiId()}.
     *
     * @return A {@link Mono} emitting on successful completion {@link Sticker custom emoji}.
     */
    public Mono<Sticker> getEmoji() {
        return client.getCustomEmoji(emojiId);
    }

    /**
     * Gets timestamp until which this status is valid, if present, otherwise will be available forever.
     *
     * @return The timestamp until which this status is valid, if present, otherwise will be available forever.
     */
    public Optional<Instant> getUntilTimestamp() {
        return Optional.ofNullable(untilTimestamp);
    }

    @Override
    public String toString() {
        return "EmojiStatus{" +
                "emojiId=" + emojiId +
                ", untilTimestamp=" + untilTimestamp +
                '}';
    }
}
