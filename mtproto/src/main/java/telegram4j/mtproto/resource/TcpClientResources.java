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
package telegram4j.mtproto.resource;

import io.netty.channel.EventLoopGroup;
import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * An object with TCP client parameters.
 * <p>
 * This object contains running {@link EventLoopGroup}
 * and must be disposed after client closing.
 *
 * @apiNote This class is immutable and thread-safe.
 */
public final class TcpClientResources {
    @Nullable
    private final ProxyResources proxyResources;
    private final EventLoopResources eventLoopResources;
    private final EventLoopGroup eventLoopGroup;

    private TcpClientResources(@Nullable ProxyResources proxyResources,
                               EventLoopResources eventLoopResources,
                               EventLoopGroup eventLoopGroup) {
        this.proxyResources = proxyResources;
        this.eventLoopResources = eventLoopResources;
        this.eventLoopGroup = eventLoopGroup;
    }

    /** {@return The proxy client parameters} if present */
    public Optional<ProxyResources> proxyProvider() {
        return Optional.ofNullable(proxyResources);
    }

    /** {@return The parameters of netty transport} */
    public EventLoopResources eventLoopResources() {
        return eventLoopResources;
    }

    /** {@return The event loop group for all socket channels} */
    public EventLoopGroup eventLoopGroup() {
        return eventLoopGroup;
    }

    /**
     * Creates new {@code TcpClientResources} preferring
     * to use native transport depending on {@code telegram4j.netty.native} system property.
     * By default, it's {@code true}.
     *
     * @see EventLoopResources#create()
     * @see TcpClientResources#create(boolean)
     * @return A new instance of {@code TcpClientResources}.
     */
    public static TcpClientResources create() {
        return create(Transports.preferNative);
    }

    /**
     * Creates new {@code TcpClientResources} preferring
     * to use native transport.
     *
     * @param preferNative Whether native transport is preferred.
     * @see EventLoopResources#create(boolean)
     * @return A new instance of {@code TcpClientResources}.
     */
    public static TcpClientResources create(boolean preferNative) {
        return builder()
                .eventLoopResources(EventLoopResources.create(preferNative))
                .build();
    }

    /**
     * Returns a new builder to create {@link TcpClientResources}.
     *
     * @return A new instance of builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class to create instances of {@link TcpClientResources}.
     *
     * @apiNote This class is mutable and not thread-safe. Use it locally
     * or with synchronization.
     */
    public static class Builder {
        @Nullable
        ProxyResources proxyResources;
        @Nullable
        EventLoopResources eventLoopResources;

        private Builder() {}

        /**
         * Configures proxy for clients.
         *
         * @param proxyResources The settings of proxy client.
         * @return This builder.
         */
        public Builder proxyResources(ProxyResources proxyResources) {
            this.proxyResources = Objects.requireNonNull(proxyResources);
            return this;
        }

        /**
         * Configures event loop settings.
         *
         * @param eventLoopResources The netty event loop settings.
         * @return This builder.
         */
        public Builder eventLoopResources(EventLoopResources eventLoopResources) {
            this.eventLoopResources = Objects.requireNonNull(eventLoopResources);
            return this;
        }

        /**
         * Creates new {@code TcpClientResources} from builder parameters and
         * initializes {@link EventLoopGroup} for clients.
         *
         * @return New instance of {@code TcpClientResources} with initialized event loop group.
         */
        public TcpClientResources build() {
            if (eventLoopResources == null) {
                eventLoopResources = EventLoopResources.create();
            }

            var eventLoopGroup = eventLoopResources.createEventLoopGroup();
            return new TcpClientResources(proxyResources, eventLoopResources, eventLoopGroup);
        }
    }
}
