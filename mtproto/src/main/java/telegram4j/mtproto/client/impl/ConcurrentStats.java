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
package telegram4j.mtproto.client.impl;

import telegram4j.mtproto.client.ImmutableStats;
import telegram4j.mtproto.client.MTProtoClient;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Instant;
import java.util.Optional;

final class ConcurrentStats implements MTProtoClient.Stats {
    static final VarHandle QUERIES_COUNT;

    static {
        try {
            var l = MethodHandles.lookup();
            QUERIES_COUNT = l.findVarHandle(ConcurrentStats.class, "queriesCount", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    volatile Instant lastQueryTimestamp;
    volatile int queriesCount;

    void addQueriesCount(int amount) {
        QUERIES_COUNT.getAndAdd(this, amount);
    }

    void incrementQueriesCount() {
        QUERIES_COUNT.getAndAdd(this, 1);
    }

    void decrementQueriesCount() {
        QUERIES_COUNT.getAndAdd(this, -1);
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
    public MTProtoClient.Stats copy() {
        return new ImmutableStats(lastQueryTimestamp, queriesCount);
    }

    @Override
    public String toString() {
        return "Stats{" +
                "lastQueryTimestamp=" + lastQueryTimestamp +
                ", queriesCount=" + queriesCount +
                '}';
    }
}
