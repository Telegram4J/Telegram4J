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
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Subtype of {@code EventLoopResources} which
 * creates NIO transport for TCP client.
 */
public non-sealed class NioEventLoopResources implements EventLoopResources {
    /**
     * Creates a new {@code NioEventLoopGroup} for TCP client.
     * <p>
     * Default implementation of method creates new group
     * with thread-pool size of {@link EventLoopResources#DEFAULT_IO_WORKER_COUNT}
     * and default loop parameters.
     *
     * @return A new {@code NioEventLoopGroup}.
     */
    @Override
    public NioEventLoopGroup createEventLoopGroup() {
        var threadFactory = new DefaultThreadFactory("t4j-nio", true);
        return new NioEventLoopGroup(DEFAULT_IO_WORKER_COUNT, threadFactory);
    }

    /** {@return A lazy factory to create {@code NioSocketChannel}} */
    @Override
    public ChannelFactory<? extends NioSocketChannel> getChannelFactory() {
        return NioSocketChannel::new;
    }
}
