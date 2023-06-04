package telegram4j.mtproto.resource;

import io.netty.util.internal.SystemPropertyUtil;

final class Transports {

    static final boolean preferNative = SystemPropertyUtil.getBoolean("telegram4j.netty.native", true);
    static final boolean hasNative;

    static {
        hasNative = Epoll.available || KQueue.available;
    }

    static class Epoll {
        static final boolean available;

        static {
            boolean status;
            try {
                Class.forName("io.netty.channel.epoll.Epoll");
                status = io.netty.channel.epoll.Epoll.isAvailable();
            } catch (ClassNotFoundException e) {
                status = false;
            }

            available = status;
        }
    }

    static class KQueue {
        static final boolean available;

        static {
            boolean status;
            try {
                Class.forName("io.netty.channel.kqueue.KQueue");
                status = io.netty.channel.kqueue.KQueue.isAvailable();
            } catch (ClassNotFoundException e) {
                status = false;
            }

            available = status;
        }
    }
}
