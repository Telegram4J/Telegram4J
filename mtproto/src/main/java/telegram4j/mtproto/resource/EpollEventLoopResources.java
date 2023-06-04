package telegram4j.mtproto.resource;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;

import java.util.concurrent.ThreadFactory;

final class EpollEventLoopResources implements EventLoopResources {
    static final EventLoopResources instance = new EpollEventLoopResources();

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new EpollEventLoopGroup(nThreads, threadFactory);
    }

    @Override
    public ChannelFactory<? extends Channel> getChannelFactory() {
        return EpollSocketChannel::new;
    }

    @Override
    public String getGroupPrefix() {
        return "epoll";
    }
}
