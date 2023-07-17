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
