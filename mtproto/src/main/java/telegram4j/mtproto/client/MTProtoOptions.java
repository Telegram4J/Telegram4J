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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public record MTProtoOptions(TcpClientResources tcpClientResources, PublicRsaKeyRegister publicRsaKeyRegister,
                             DhPrimeChecker dhPrimeChecker, TransportFactory transportFactory, StoreLayout storeLayout,
                             List<ResponseTransformer> responseTransformers,
                             InvokeWithLayer<Object, InitConnection<Object, TlMethod<?>>> initConnection,
                             int gzipWrappingSizeThreshold, ExecutorService resultPublisher,
                             Scheduler updatesPublisher) {

    public MTProtoOptions {
        Objects.requireNonNull(tcpClientResources);
        Objects.requireNonNull(publicRsaKeyRegister);
        Objects.requireNonNull(dhPrimeChecker);
        Objects.requireNonNull(transportFactory);
        Objects.requireNonNull(storeLayout);
        Objects.requireNonNull(responseTransformers);
        Objects.requireNonNull(initConnection);
        Objects.requireNonNull(resultPublisher);
        Objects.requireNonNull(updatesPublisher);
    }
}
