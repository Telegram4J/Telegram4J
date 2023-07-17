package telegram4j.mtproto.resource;

import io.netty.channel.ChannelFactory;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Subtype of {@code EventLoopResources} which
 * creates Linux-specific epoll transport for TCP client.
 */
public non-sealed class EpollEventLoopResources implements EventLoopResources {
    /**
     * Creates a new {@code EpollEventLoopGroup} for TCP client.
     * <p>
     * Default implementation of method creates new group
     * with thread-pool size of {@link EventLoopResources#DEFAULT_IO_WORKER_COUNT}
     * and default loop parameters.
     *
     * @return A new {@code EpollEventLoopGroup}.
     */
    @Override
    public EpollEventLoopGroup createEventLoopGroup() {
        var threadFactory = new DefaultThreadFactory("t4j-epoll", true);
        return new EpollEventLoopGroup(DEFAULT_IO_WORKER_COUNT, threadFactory);
    }

    /** {@return A lazy factory to create {@code EpollSocketChannel}} */
    @Override
    public ChannelFactory<? extends EpollSocketChannel> getChannelFactory() {
        return EpollSocketChannel::new;
    }
}
