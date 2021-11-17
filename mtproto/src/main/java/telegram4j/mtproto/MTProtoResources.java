package telegram4j.mtproto;

import reactor.netty.tcp.TcpClient;
import telegram4j.mtproto.transport.IntermediateTransport;
import telegram4j.mtproto.transport.Transport;

import java.util.function.Supplier;

public class MTProtoResources {

    public static final Supplier<TcpClient> DEFAULT_TCP_CLIENT = TcpClient::create;
    public static final Supplier<Transport> DEFAULT_TRANSPORT = IntermediateTransport::new;

    private final TcpClient tcpClient;
    private final boolean test;
    private final Transport transport;

    public MTProtoResources() {
        this.transport = DEFAULT_TRANSPORT.get();
        this.tcpClient = DEFAULT_TCP_CLIENT.get();
        this.test = false;
    }

    public MTProtoResources(TcpClient tcpClient, boolean test, Transport transport) {
        this.tcpClient = tcpClient;
        this.test = test;
        this.transport = transport;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public boolean isTest() {
        return test;
    }

    public Transport getTransport() {
        return transport;
    }
}
