package telegram4j.core.auth;

import reactor.core.publisher.Mono;
import telegram4j.core.AuthorizationResources;
import telegram4j.mtproto.client.MTProtoClientManager;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.auth.BaseAuthorization;

import java.util.Objects;

/** Base interface for implementing auth flow. */
@FunctionalInterface
public interface AuthorizationHandler {

    /**
     * Begins user authorization with specified resources.
     * Implementation may emit empty signals to disconnect client and
     * cancel bootstrap.
     *
     * @param resources The resources for authorisation.
     * @return A {@link Mono} which emits {@link BaseAuthorization} on successful completion or empty signals
     * to cancel bootstrap and close client.
     */
    Mono<BaseAuthorization> process(Resources resources);

    /**
     * Value-based record with components available on auth-flow.
     *
     * @param clientGroup The client group to handle redirections and requests.
     * @param storeLayout The initialized store layout for client.
     * @param authResources The {@code apiId} and {@code apiHash} parameters of application.
     */
    record Resources(MTProtoClientManager clientGroup, StoreLayout storeLayout,
                     AuthorizationResources authResources) {

        public Resources {
            Objects.requireNonNull(clientGroup);
            Objects.requireNonNull(storeLayout);
            Objects.requireNonNull(authResources);
        }
    }
}
