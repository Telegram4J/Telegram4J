package telegram4j.mtproto;

import reactor.core.publisher.Sinks;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.RetryBackoffSpec;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.Transport;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class MTProtoOptions {
    protected final DataCenter datacenter;
    protected final TcpClient tcpClient;
    protected final Supplier<PublicRsaKeyRegister> publicRsaKeyRegister;
    protected final Supplier<Transport> transport;
    protected final StoreLayout storeLayout;
    protected final Sinks.EmitFailureHandler emissionHandler;
    protected final RetryBackoffSpec connectionRetry;
    protected final RetryBackoffSpec authRetry;
    protected final List<ResponseTransformer> responseTransformers;
    protected final InvokeWithLayer<Object, InitConnection<Object, TlMethod<?>>> initConnection;
    protected final int gzipWrappingSizeThreshold;

    public MTProtoOptions(DataCenter datacenter, TcpClient tcpClient,
                          Supplier<PublicRsaKeyRegister> publicRsaKeyRegister, Supplier<Transport> transport,
                          StoreLayout storeLayout, Sinks.EmitFailureHandler emissionHandler,
                          RetryBackoffSpec connectionRetry, RetryBackoffSpec authRetry,
                          List<ResponseTransformer> responseTransformers,
                          InvokeWithLayer<Object, InitConnection<Object, TlMethod<?>>> initConnection,
                          int gzipWrappingSizeThreshold) {
        this.datacenter = Objects.requireNonNull(datacenter);
        this.tcpClient = Objects.requireNonNull(tcpClient);
        this.publicRsaKeyRegister = Objects.requireNonNull(publicRsaKeyRegister);
        this.transport = Objects.requireNonNull(transport);
        this.storeLayout = Objects.requireNonNull(storeLayout);
        this.emissionHandler = Objects.requireNonNull(emissionHandler);
        this.connectionRetry = Objects.requireNonNull(connectionRetry);
        this.authRetry = Objects.requireNonNull(authRetry);
        this.responseTransformers = Objects.requireNonNull(responseTransformers);
        this.initConnection = Objects.requireNonNull(initConnection);
        this.gzipWrappingSizeThreshold = gzipWrappingSizeThreshold;
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

    public Supplier<PublicRsaKeyRegister> getPublicRsaKeyRegister() {
        return publicRsaKeyRegister;
    }

    public StoreLayout getStoreLayout() {
        return storeLayout;
    }

    public Sinks.EmitFailureHandler getEmissionHandler() {
        return emissionHandler;
    }

    public RetryBackoffSpec getConnectionRetry() {
        return connectionRetry;
    }

    public RetryBackoffSpec getAuthRetry() {
        return authRetry;
    }

    public List<ResponseTransformer> getResponseTransformers() {
        return responseTransformers;
    }

    public InvokeWithLayer<Object, InitConnection<Object, TlMethod<?>>> getInitConnection() {
        return initConnection;
    }

    public int getGzipWrappingSizeThreshold() {
        return gzipWrappingSizeThreshold;
    }
}
