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

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;

/**
 * A resources of netty parameters used to configure TCP clients.
 *
 * @implSpec Implementations of this resource must be thread-safe.
 */
public sealed interface EventLoopResources
        permits EpollEventLoopResources, KQueueEventLoopResources, NioEventLoopResources {

    /**
     * A constant value from system property {@code telegram4j.netty.ioWorkerCount}
     * which specified default count of I/O threads for event loop groups. By default, it does not exceed 4.
     */
    int DEFAULT_IO_WORKER_COUNT = Integer.getInteger(
            "telegram4j.netty.ioWorkerCount",
            Math.min(Runtime.getRuntime().availableProcessors(), 4));

    /**
     * Creates {@code NioEventLoopResources} using
     * default event loop and socket channel parameters.
     *
     * @return A new instance of {@code NioEventLoopResources}.
     */
    static NioEventLoopResources nio() {
        return new NioEventLoopResources();
    }

    /**
     * Creates new {@code EventLoopResources}
     * preferring native transport if available.
     * <p>
     * This method uses system property {@code telegram4j.netty.native} to decide preference.
     * By default, it's {@code true}
     *
     * @see #create(boolean)
     * @return A new {@code EventLoopResources} with native transport if any.
     */
    static EventLoopResources create() {
        return create(Transports.preferNative);
    }

    /**
     * Creates new {@code EventLoopResources}
     * preferring native transport if available.
     *
     * @param preferNative Whether native transport is preferred.
     * @return A new {@code EventLoopResources} with native transport if any.
     */
    static EventLoopResources create(boolean preferNative) {
        if (preferNative) {
            if (Transports.Epoll.available) {
                return new EpollEventLoopResources();
            } else if (Transports.KQueue.available) {
                return new KQueueEventLoopResources();
            }
        }
        return new NioEventLoopResources();
    }

    /**
     * Creates a new {@code EventLoopGroup} for TCP client.
     * <p>
     * Returning event loop group must be compatible with {@link SocketChannel}s
     * created by {@link #getChannelFactory()}.
     *
     * @return A new {@code EventLoopGroup}.
     */
    EventLoopGroup createEventLoopGroup();

    /** {@return A lazy factory to create {@code SocketChannel}} */
    ChannelFactory<? extends SocketChannel> getChannelFactory();
}
