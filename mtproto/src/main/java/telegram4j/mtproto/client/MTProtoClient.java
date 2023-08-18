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

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.ResponseTransformer;
import telegram4j.mtproto.transport.TransportFactory;
import telegram4j.tl.Config;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.help.GetConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static telegram4j.mtproto.internal.Preconditions.requireArgument;

/**
 * Interface for MTProto client implementations with minimal method set.
 *
 * @implSpec The client must implement RPC, auth, update logics and auto-reconnects.
 */
public interface MTProtoClient {

    /**
     * Gets a {@link Mono} with empty signals which starts client on subscribe.
     *
     * @return A {@link Mono} which emitting signals when client ready to handle requests.
     */
    Mono<Void> connect();

    /**
     * Send api request with result awaiting.
     *
     * @param <R> Type of result.
     * @param method An api request.
     * @return A {@link Mono} emitting signals with result on successful completion.
     */
    <R> Mono<R> send(TlMethod<? extends R> method);

    /**
     * Gets the client datacenter.
     *
     * @return The {@link DataCenter} to which the client is configured.
     */
    DataCenter dc();

    /**
     * Gets the type of destination of client.
     *
     * @return The type of destination of client.
     */
    DcId.Type type();

    /**
     * Gets mutable statistic for client.
     *
     * @return The statistic for client.
     */
    Stats stats();

    /**
     * Gets a {@link Mono} which closes client and emitting empty signals.
     * After this operation, the client can't be used to send queries.
     *
     * @return A {@link Mono} emitting empty signals on successful completion.
     */
    Mono<Void> close();

    /**
     * Gets a {@link Mono} which emits signals on client close.
     *
     * @return A {@link Mono} emitting empty signals on successful client close.
     */
    Mono<Void> onClose();

    /** Interface for client statistic. */
    interface Stats {

        /**
         * Gets timestamp of last {@link #send(TlMethod)} call, if present.
         *
         * @return The timestamp of last send query call, if present.
         */
        Optional<Instant> lastQueryTimestamp();

        /**
         * Gets current count of pending queries.
         *
         * @return The current count of pending queries.
         */
        int queriesCount();

        /**
         * Creates new immutable copy of this statistics.
         *
         * @return A new immutable copy of this statistics.
         */
        default Stats copy() {
            return new ImmutableStats(lastQueryTimestamp().orElse(null), queriesCount());
        }
    }

    record Options(TransportFactory transportFactory,
                   InvokeWithLayer<Config, InitConnection<Config, GetConfig>> initConnection,
                   Duration pingInterval, ReconnectionStrategy reconnectionStrategy,
                   int gzipCompressionSizeThreshold, List<ResponseTransformer> responseTransformers,
                   Duration authKeyLifetime) {

        public Options {
            requireNonNull(transportFactory);
            requireNonNull(initConnection);
            requireNonNull(pingInterval);
            requireNonNull(reconnectionStrategy);
            requireNonNull(responseTransformers);
            requireArgument(!authKeyLifetime.isNegative(), "authKeyLifetime must be positive or zero");
        }
    }
}
