package telegram4j.mtproto.resource;

import io.netty.channel.ChannelFactory;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Subtype of {@code EventLoopResources} which
 * creates MacOS/BSD native transport for TCP client.
 */
public non-sealed class KQueueEventLoopResources implements EventLoopResources {
    /**
     * Creates a new {@code KQueueEventLoopGroup} for TCP client.
     * <p>
     * Default implementation of method creates new group
     * with thread-pool size of {@link EventLoopResources#DEFAULT_IO_WORKER_COUNT}
     * and default loop parameters.
     *
     * @return A new {@code KQueueEventLoopGroup}.
     */
    @Override
    public KQueueEventLoopGroup createEventLoopGroup() {
        var threadFactory = new DefaultThreadFactory("t4j-kqueue", true);
        return new KQueueEventLoopGroup(DEFAULT_IO_WORKER_COUNT, threadFactory);
    }

    /** {@return A lazy factory to create {@code KQueueSocketChannel}} */
    @Override
    public ChannelFactory<? extends KQueueSocketChannel> getChannelFactory() {
        return KQueueSocketChannel::new;
    }
}
