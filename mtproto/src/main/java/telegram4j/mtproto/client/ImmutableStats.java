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
package telegram4j.mtproto.client;

import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Optional;

/** Immutable implementation of {@code MTProtoClient.Stats}. */
public class ImmutableStats implements MTProtoClient.Stats {
    @Nullable
    protected final Instant lastQueryTimestamp;
    protected final int queriesCount;

    public ImmutableStats(@Nullable Instant lastQueryTimestamp, int queriesCount) {
        this.lastQueryTimestamp = lastQueryTimestamp;
        this.queriesCount = queriesCount;
    }

    @Override
    public Optional<Instant> lastQueryTimestamp() {
        return Optional.ofNullable(lastQueryTimestamp);
    }

    @Override
    public int queriesCount() {
        return queriesCount;
    }

    @Override
    public String toString() {
        return "ImmutableStats{" +
                "lastQueryTimestamp=" + lastQueryTimestamp +
                ", queriesCount=" + queriesCount +
                '}';
    }
}
