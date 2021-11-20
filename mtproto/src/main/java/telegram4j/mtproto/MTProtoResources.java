package telegram4j.mtproto;

import reactor.netty.tcp.TcpClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.mtproto.transport.IntermediateTransport;
import telegram4j.mtproto.transport.Transport;

import java.util.function.Supplier;

public class MTProtoResources {
    public static final Supplier<StoreLayout> DEFAULT_STORE_LAYOUT = StoreLayoutImpl::new;
    public static final Supplier<TcpClient> DEFAULT_TCP_CLIENT = TcpClient::create;
    public static final Supplier<Transport> DEFAULT_TRANSPORT = IntermediateTransport::new;

    private final StoreLayout storeLayout;
    private final TcpClient tcpClient;
    private final boolean test;
    private final Transport transport;

    public MTProtoResources() {
        this(DEFAULT_STORE_LAYOUT.get(), DEFAULT_TCP_CLIENT.get(), false, DEFAULT_TRANSPORT.get());
    }

    public MTProtoResources(StoreLayout storeLayout, TcpClient tcpClient, boolean test, Transport transport) {
        this.storeLayout = storeLayout;
        this.tcpClient = tcpClient;
        this.test = test;
        this.transport = transport;
    }

    public StoreLayout getStoreLayout() {
        return storeLayout;
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
