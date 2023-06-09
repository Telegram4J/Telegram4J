package telegram4j.mtproto.client;

import reactor.core.scheduler.Scheduler;
import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.ResponseTransformer;
import telegram4j.mtproto.auth.DhPrimeChecker;
import telegram4j.mtproto.resource.TcpClientResources;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.TransportFactory;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.request.InitConnection;
import telegram4j.tl.request.InvokeWithLayer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.Objects.requireNonNull;

public record MTProtoOptions(TcpClientResources tcpClientResources, PublicRsaKeyRegister publicRsaKeyRegister,
                             DhPrimeChecker dhPrimeChecker, TransportFactory transportFactory, StoreLayout storeLayout,
                             List<ResponseTransformer> responseTransformers,
                             InvokeWithLayer<Object, InitConnection<Object, TlMethod<?>>> initConnection,
                             int gzipWrappingSizeThreshold, ExecutorService resultPublisher,
                             Scheduler updatesPublisher, Duration reconnectionInterval) {

    public MTProtoOptions {
        requireNonNull(tcpClientResources);
        requireNonNull(publicRsaKeyRegister);
        requireNonNull(dhPrimeChecker);
        requireNonNull(transportFactory);
        requireNonNull(storeLayout);
        requireNonNull(responseTransformers);
        requireNonNull(initConnection);
        requireNonNull(resultPublisher);
        requireNonNull(updatesPublisher);
        requireNonNull(reconnectionInterval);
    }
}
