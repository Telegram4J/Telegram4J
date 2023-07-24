package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.auth.BaseAuthorization;

import java.util.Objects;

@FunctionalInterface
public interface AuthorizationHandler extends AuthorisationHandler {

    /**
     * Begins user authorization with specified resources.
     * Implementation may emit empty signals to disconnect client and
     * cancel bootstrap.
     *
     * @param resources The resources for authorisation.
     * @return A {@link Mono} which emits {@link BaseAuthorization} on successful completion or empty signals
     * to disconnect client.
     */
    Mono<BaseAuthorization> process(Resources resources);

    @Override
    default Mono<BaseAuthorization> process(MTProtoClientGroup clientGroup, StoreLayout storeLayout, AuthorizationResources authResources) {
        throw new UnsupportedOperationException();
    }

    record Resources(MTProtoClientGroup clientGroup, StoreLayout storeLayout,
                     AuthorizationResources authResources) {

        public Resources {
            Objects.requireNonNull(clientGroup);
            Objects.requireNonNull(storeLayout);
            Objects.requireNonNull(authResources);
        }
    }
}
