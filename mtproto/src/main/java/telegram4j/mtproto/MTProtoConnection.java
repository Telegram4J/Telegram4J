package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;
import reactor.util.concurrent.Queues;

class MTProtoConnection {
    private final Connection connection;
    private final Sinks.Many<ByteBuf> receiver;
    private final DataCenter dataCenter;

    MTProtoConnection(Connection connection, DataCenter dataCenter) {
        this.dataCenter = dataCenter;
        this.connection = connection;

        this.receiver = newEmitterSink();
    }

    private static <T> Sinks.Many<T> newEmitterSink() {
        return Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    public Connection getConnection() {
        return connection;
    }

    public Sinks.Many<ByteBuf> getReceiver() {
        return receiver;
    }

    public DataCenter getDataCenter() {
        return dataCenter;
    }
}
