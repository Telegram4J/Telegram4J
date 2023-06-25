package telegram4j.mtproto.resource;

import io.netty.channel.ChannelFactory;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public non-sealed class KQueueEventLoopResources implements EventLoopResources {
    @Override
    public KQueueEventLoopGroup createEventLoopGroup() {
        var threadFactory = new DefaultThreadFactory("t4j-kqueue", true);
        return new KQueueEventLoopGroup(DEFAULT_IO_WORKER_COUNT, threadFactory);
    }

    @Override
    public ChannelFactory<? extends KQueueSocketChannel> getChannelFactory() {
        return KQueueSocketChannel::new;
    }
}
