package telegram4j.mtproto.resource;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;

public sealed interface EventLoopResources
        permits EpollEventLoopResources, KQueueEventLoopResources, NioEventLoopResources {

    int DEFAULT_IO_WORKER_COUNT = Integer.getInteger(
            "telegram4j.netty.ioWorkerCount",
            Math.min(Runtime.getRuntime().availableProcessors(), 4));

    static EventLoopResources nio() {
        return new NioEventLoopResources();
    }

    static EventLoopResources create() {
        return create(Transports.preferNative);
    }

    static EventLoopResources create(boolean preferNative) {
        if (preferNative) {
            if (Transports.Epoll.available) {
                return new EpollEventLoopResources();
            } else if (Transports.KQueue.available) {
                return new KQueueEventLoopResources();
            }
        }
        return new NioEventLoopResources();
    }

    EventLoopGroup createEventLoopGroup();

    ChannelFactory<? extends Channel> getChannelFactory();
}
