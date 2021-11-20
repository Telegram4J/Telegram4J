package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;

public class StoreLayoutImpl implements StoreLayout {

    private volatile AuthorizationKeyHolder authorizationKey;

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey() {
        return Mono.justOrEmpty(authorizationKey);
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        return Mono.fromRunnable(() -> this.authorizationKey = authorizationKey);
    }
}
