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

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.client.ReconnectionContext;

import java.time.Duration;
import java.util.Optional;

final class ReconnectionContextImpl implements ReconnectionContext {
    private int iteration;
    @Nullable
    private Duration lastBackoff;
    private Throwable exception;

    // volatile for multi-thread access in MTProtoClient.close()
    private volatile boolean resume = true;

    public void resetAfterConnect() {
        iteration = 0;
        exception = null;
        lastBackoff = null;
    }

    public void reset() {
        resetAfterConnect();
        resume = false;
    }

    public void increment() {
        iteration = Math.incrementExact(iteration);
    }

    public void setException(@Nullable Throwable exception) {
        this.exception = exception;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    public void setLastBackoff(Duration lastBackoff) {
        this.lastBackoff = lastBackoff;
    }

    public boolean isResume() {
        return resume;
    }

    @Nullable
    public Throwable cause() {
        return exception;
    }

    @Override
    public int iteration() {
        return iteration;
    }

    @Override
    public Optional<Duration> lastBackoff() {
        return Optional.ofNullable(lastBackoff);
    }

    @Override
    public Optional<Throwable> exception() {
        return Optional.ofNullable(exception);
    }

    @Override
    public String toString() {
        return "ReconnectionContext{" +
                "iteration=" + iteration +
                ", lastBackoff=" + lastBackoff +
                ", exception=" + exception +
                '}';
    }
}
