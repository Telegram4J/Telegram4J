package telegram4j.mtproto.client;

import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.auth.DhPrimeChecker;
import telegram4j.mtproto.resource.TcpClientResources;
import telegram4j.mtproto.store.StoreLayout;

import java.util.concurrent.ExecutorService;

import static java.util.Objects.requireNonNull;

public record MTProtoOptions(TcpClientResources tcpClientResources, PublicRsaKeyRegister publicRsaKeyRegister,
                             DhPrimeChecker dhPrimeChecker, StoreLayout storeLayout,
                             ExecutorService resultPublisher, boolean disposeResultPublisher) {

    public MTProtoOptions {
        requireNonNull(tcpClientResources);
        requireNonNull(publicRsaKeyRegister);
        requireNonNull(dhPrimeChecker);
        requireNonNull(storeLayout);
        requireNonNull(resultPublisher);
    }
}
