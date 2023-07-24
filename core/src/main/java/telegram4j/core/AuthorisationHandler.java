package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.auth.BaseAuthorization;

public interface AuthorisationHandler {
    @Deprecated(forRemoval = true)
    Mono<BaseAuthorization> process(MTProtoClientGroup clientGroup, StoreLayout storeLayout,
                                    AuthorizationResources authResources);
}
