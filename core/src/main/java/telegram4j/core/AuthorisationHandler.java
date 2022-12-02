package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.auth.BaseAuthorization;

@FunctionalInterface
public interface AuthorisationHandler {

    /**
     * Begins user authorization with specified resources.
     *
     * <p> Implementation may emit empty signals to disconnect client and
     * cancel bootstrap. This method may be invoked again after redirecting to correct DC.
     * All RPC errors with code 303 should not be handled.
     *
     * @return A {@link Mono} which emits {@link BaseAuthorization} on successful completion or empty signals
     * to disconnect client.
     */
    Mono<BaseAuthorization> process(MTProtoClientGroup clientGroup, StoreLayout storeLayout,
                                    AuthorizationResources authResources);
}
