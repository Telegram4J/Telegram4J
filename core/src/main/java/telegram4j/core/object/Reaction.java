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

import java.util.Objects;
import java.util.Optional;

public final class Reaction {
    private final long emojiId;
    @Nullable
    private final String emoticon;

    public Reaction(long emojiId) {
        this.emojiId = emojiId;
        this.emoticon = null;
    }

    public Reaction(String emoticon) {
        this.emoticon = Objects.requireNonNull(emoticon);
        this.emojiId = 0;
    }

    /**
     * Gets id of custom emoji of this reaction, if its custom emoji.
     *
     * @return The id of custom emoji of this reaction, if its custom emoji.
     */
    public Optional<Long> getEmojiId() {
        return emoticon == null ? Optional.of(emojiId) : Optional.empty();
    }

    /**
     * Gets emoticon of this reaction, if it's not custom emoji.
     *
     * @return The emoticon of this reaction, if it's not custom emoji.
     */
    public Optional<String> getEmoticon() {
        return Optional.ofNullable(emoticon);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof Reaction r)) return false;
        return emojiId == r.emojiId && Objects.equals(emoticon, r.emoticon);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(emojiId) + Objects.hashCode(emoticon);
    }

    @Override
    public String toString() {
        return "Reaction{" + (emoticon != null ? "emoticon='" + emoticon + '\'' : "emojiId=" + emojiId) + '}';
    }
}
