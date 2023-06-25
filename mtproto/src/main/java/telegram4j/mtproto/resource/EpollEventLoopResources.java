package telegram4j.mtproto.resource;

import io.netty.channel.ChannelFactory;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public non-sealed class EpollEventLoopResources implements EventLoopResources {
    @Override
    public EpollEventLoopGroup createEventLoopGroup() {
        var threadFactory = new DefaultThreadFactory("t4j-epoll", true);
        return new EpollEventLoopGroup(DEFAULT_IO_WORKER_COUNT, threadFactory);
    }

    @Override
    public ChannelFactory<? extends EpollSocketChannel> getChannelFactory() {
        return EpollSocketChannel::new;
    }
}
