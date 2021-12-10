package telegram4j.mtproto;

import reactor.core.publisher.Sinks;
import reactor.netty.tcp.TcpClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.Transport;

public class MTProtoOptions {
    private final DataCenter datacenter;
    private final TcpClient tcpClient;
    private final Transport transport;
    private final StoreLayout storeLayout;
    private final int acksSendThreshold;
    private final Sinks.EmitFailureHandler emissionHandler;

    public MTProtoOptions(DataCenter datacenter, TcpClient tcpClient,
                          Transport transport, StoreLayout storeLayout,
                          int acksSendThreshold, Sinks.EmitFailureHandler emissionHandler) {
        this.datacenter = datacenter;
        this.tcpClient = tcpClient;
        this.transport = transport;
        this.storeLayout = storeLayout;
        this.acksSendThreshold = acksSendThreshold;
        this.emissionHandler = emissionHandler;
    }

    public DataCenter getDatacenter() {
        return datacenter;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public Transport getTransport() {
        return transport;
    }

    public StoreLayout getStoreLayout() {
        return storeLayout;
    }

    public int getAcksSendThreshold() {
        return acksSendThreshold;
    }

    public Sinks.EmitFailureHandler getEmissionHandler() {
        return emissionHandler;
    }
}
