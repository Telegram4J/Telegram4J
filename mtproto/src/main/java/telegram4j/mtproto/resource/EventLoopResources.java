package telegram4j.mtproto.resource;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;

import java.util.concurrent.ThreadFactory;

public sealed interface EventLoopResources
        permits EpollEventLoopResources, KQueueEventLoopResources, NioEventLoopResources {

    int DEFAULT_IO_WORKER_COUNT = Integer.parseInt(System.getProperty(
            "telegram4j.netty.ioWorkerCount",
            "" + Math.max(Runtime.getRuntime().availableProcessors(), 4)));

    static EventLoopResources nio() {
        return NioEventLoopResources.instance;
    }

    static EventLoopResources create() {
        return create(Transports.preferNative);
    }

    static EventLoopResources create(boolean preferNative) {
        if (preferNative) {
            if (Transports.Epoll.available) {
                return EpollEventLoopResources.instance;
            } else if (Transports.KQueue.available) {
                return KQueueEventLoopResources.instance;
            }
        }
        return NioEventLoopResources.instance;
    }

    EventLoopGroup createEventLoopGroup(int nThreads, ThreadFactory threadFactory);

    ChannelFactory<? extends Channel> getChannelFactory();

    String getGroupPrefix();
}
