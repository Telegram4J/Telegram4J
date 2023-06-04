package telegram4j.mtproto.resource;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

public final class TcpClientResources {
    private final ThreadFactory threadFactory;
    private final EventLoopResources eventLoopResources;
    private final EventLoopGroup eventLoopGroup;

    private TcpClientResources(ThreadFactory threadFactory, EventLoopResources eventLoopResources,
                               EventLoopGroup eventLoopGroup) {
        this.threadFactory = threadFactory;
        this.eventLoopResources = eventLoopResources;
        this.eventLoopGroup = eventLoopGroup;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public EventLoopResources getEventLoopResources() {
        return eventLoopResources;
    }

    public EventLoopGroup getEventLoopGroup() {
        return eventLoopGroup;
    }

    public static TcpClientResources create() {
        return create(Transports.preferNative, EventLoopResources.DEFAULT_IO_WORKER_COUNT);
    }

    public static TcpClientResources create(boolean preferNative, int ioWorkerCount) {
        EventLoopResources eventLoopResources = EventLoopResources.create(preferNative);

        var threadFactory = new DefaultThreadFactory("t4j-" + eventLoopResources.getGroupPrefix());
        var eventLoopGroup = eventLoopResources.createEventLoopGroup(ioWorkerCount, threadFactory);

        return new TcpClientResources(threadFactory, eventLoopResources, eventLoopGroup);
    }
}
