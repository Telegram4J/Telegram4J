package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;

class MTProtoConnection {
    private final Connection connection;
    private final Sinks.Many<ByteBuf> receiver;
    private final DataCenter dataCenter;

    MTProtoConnection(Connection connection, Sinks.Many<ByteBuf> receiver, DataCenter dataCenter) {
        this.connection = connection;
        this.receiver = receiver;
        this.dataCenter = dataCenter;
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
