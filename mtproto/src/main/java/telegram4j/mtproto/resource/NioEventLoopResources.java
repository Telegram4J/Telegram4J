package telegram4j.mtproto.resource;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ThreadFactory;

final class NioEventLoopResources implements EventLoopResources {
    static final EventLoopResources instance = new NioEventLoopResources();

    @Override
    public EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new NioEventLoopGroup(nThreads, threadFactory);
    }

    @Override
    public ChannelFactory<? extends Channel> getChannelFactory() {
        return NioSocketChannel::new;
    }

    @Override
    public String getGroupPrefix() {
        return "nio";
    }
}
