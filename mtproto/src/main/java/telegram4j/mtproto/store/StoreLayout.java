package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;

public interface StoreLayout {

    Mono<AuthorizationKeyHolder> getAuthorizationKey();

    Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey);
}
