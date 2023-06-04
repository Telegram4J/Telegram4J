package telegram4j.mtproto.client;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;
import reactor.util.retry.RetryBackoffSpec;
import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.ResponseTransformer;
import telegram4j.mtproto.auth.DhPrimeChecker;
import telegram4j.mtproto.resource.TcpClientResources;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.TransportFactory;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MTProtoOptions {
    protected final TcpClientResources tcpClientResources;
    @Nullable
    protected final PublicRsaKeyRegister publicRsaKeyRegister;
    @Nullable
    protected final DhPrimeChecker dhPrimeChecker;
    protected final TransportFactory transport;
    protected final StoreLayout storeLayout;
    protected final RetryBackoffSpec connectionRetry;
    protected final RetryBackoffSpec authRetry;
    protected final List<ResponseTransformer> responseTransformers;
    protected final InvokeWithLayer<Object, InitConnection<Object, TlMethod<?>>> initConnection;
    protected final int gzipWrappingSizeThreshold;

    // TODO
    protected final Scheduler resultPublishScheduler = Schedulers.newParallel("t4j-result", 4);

    public MTProtoOptions(TcpClientResources tcpClientResources, @Nullable PublicRsaKeyRegister publicRsaKeyRegister,
                          @Nullable DhPrimeChecker dhPrimeChecker, TransportFactory transport,
                          StoreLayout storeLayout, RetryBackoffSpec connectionRetry,
                          RetryBackoffSpec authRetry, List<ResponseTransformer> responseTransformers,
                          InvokeWithLayer<Object, InitConnection<Object, TlMethod<?>>> initConnection,
                          int gzipWrappingSizeThreshold) {
        this.tcpClientResources = Objects.requireNonNull(tcpClientResources);
        this.publicRsaKeyRegister = publicRsaKeyRegister;
        this.dhPrimeChecker = dhPrimeChecker;
        this.transport = Objects.requireNonNull(transport);
        this.storeLayout = Objects.requireNonNull(storeLayout);
        this.connectionRetry = Objects.requireNonNull(connectionRetry);
        this.authRetry = Objects.requireNonNull(authRetry);
        this.responseTransformers = Objects.requireNonNull(responseTransformers);
        this.initConnection = Objects.requireNonNull(initConnection);
        this.gzipWrappingSizeThreshold = gzipWrappingSizeThreshold;
    }

    public TcpClientResources getTcpClientResources() {
        return tcpClientResources;
    }

    public TransportFactory getTransport() {
        return transport;
    }

    public Optional<PublicRsaKeyRegister> getPublicRsaKeyRegister() {
        return Optional.ofNullable(publicRsaKeyRegister);
    }

    public Optional<DhPrimeChecker> getDhPrimeChecker() {
        return Optional.ofNullable(dhPrimeChecker);
    }

    public StoreLayout getStoreLayout() {
        return storeLayout;
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

    public Scheduler getResultPublishScheduler() {
        return resultPublishScheduler;
    }
}
