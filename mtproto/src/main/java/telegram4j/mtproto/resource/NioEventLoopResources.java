package telegram4j.mtproto.resource;

import io.netty.channel.ChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public non-sealed class NioEventLoopResources implements EventLoopResources {
    @Override
    public NioEventLoopGroup createEventLoopGroup() {
        var threadFactory = new DefaultThreadFactory("t4j-nio", true);
        return new NioEventLoopGroup(DEFAULT_IO_WORKER_COUNT, threadFactory);
    }

    @Override
    public ChannelFactory<? extends NioSocketChannel> getChannelFactory() {
        return NioSocketChannel::new;
    }
}
