package telegram4j.mtproto;

import reactor.core.publisher.Sinks;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.RetryBackoffSpec;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.Transport;

import java.util.function.IntPredicate;
import java.util.function.Supplier;

public class MTProtoOptions {
    private final DataCenter datacenter;
    private final TcpClient tcpClient;
    private final Supplier<Transport> transport;
    private final StoreLayout storeLayout;
    private final int acksSendThreshold;
    private final Sinks.EmitFailureHandler emissionHandler;
    private final RetryBackoffSpec retry;
    private final RetryBackoffSpec authRetry;
    private final IntPredicate gzipPackingPredicate;

    public MTProtoOptions(DataCenter datacenter, TcpClient tcpClient,
                          Supplier<Transport> transport, StoreLayout storeLayout,
                          int acksSendThreshold, Sinks.EmitFailureHandler emissionHandler,
                          RetryBackoffSpec retry, RetryBackoffSpec authRetry,
                          IntPredicate gzipPackingPredicate) {
        this.datacenter = datacenter;
        this.tcpClient = tcpClient;
        this.transport = transport;
        this.storeLayout = storeLayout;
        this.acksSendThreshold = acksSendThreshold;
        this.emissionHandler = emissionHandler;
        this.retry = retry;
        this.authRetry = authRetry;
        this.gzipPackingPredicate = gzipPackingPredicate;
    }

    public DataCenter getDatacenter() {
        return datacenter;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public Supplier<Transport> getTransport() {
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

    public RetryBackoffSpec getRetry() {
        return retry;
    }

    public RetryBackoffSpec getAuthRetry() {
        return authRetry;
    }

    public IntPredicate getGzipPackingPredicate() {
        return gzipPackingPredicate;
    }
}
