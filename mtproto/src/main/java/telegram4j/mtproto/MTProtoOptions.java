package telegram4j.mtproto;

import reactor.core.publisher.Sinks;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.RetryBackoffSpec;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.Transport;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

public class MTProtoOptions {
    protected final DataCenter datacenter;
    protected final TcpClient tcpClient;
    protected final Supplier<Transport> transport;
    protected final StoreLayout storeLayout;
    protected final Sinks.EmitFailureHandler emissionHandler;
    protected final RetryBackoffSpec retry;
    protected final RetryBackoffSpec authRetry;
    protected final IntPredicate gzipPackingPredicate;
    protected final List<ResponseTransformer> responseTransformers;

    public MTProtoOptions(DataCenter datacenter, TcpClient tcpClient,
                          Supplier<Transport> transport, StoreLayout storeLayout, Sinks.EmitFailureHandler emissionHandler,
                          RetryBackoffSpec retry, RetryBackoffSpec authRetry,
                          IntPredicate gzipPackingPredicate, List<ResponseTransformer> responseTransformers) {
        this.datacenter = datacenter;
        this.tcpClient = tcpClient;
        this.transport = transport;
        this.storeLayout = storeLayout;
        this.emissionHandler = emissionHandler;
        this.retry = retry;
        this.authRetry = authRetry;
        this.gzipPackingPredicate = gzipPackingPredicate;
        this.responseTransformers = responseTransformers;
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

    public List<ResponseTransformer> getResponseTransformers() {
        return responseTransformers;
    }
}
