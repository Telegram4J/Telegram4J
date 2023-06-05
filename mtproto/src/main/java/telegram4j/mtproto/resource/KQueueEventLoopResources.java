package telegram4j.mtproto.resource;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;

import java.util.concurrent.ThreadFactory;

final class KQueueEventLoopResources implements EventLoopResources {
    static final EventLoopResources instance = new KQueueEventLoopResources();

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new KQueueEventLoopGroup(nThreads, threadFactory);
    }

    @Override
    public ChannelFactory<? extends Channel> getChannelFactory() {
        return KQueueSocketChannel::new;
    }

    @Override
    public String getGroupPrefix() {
        return "kqueue";
    }
}
