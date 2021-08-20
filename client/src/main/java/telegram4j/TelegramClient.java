package telegram4j;

import reactor.core.publisher.Mono;
import telegram4j.rest.DefaultRouter;
import telegram4j.rest.RestResources;
import telegram4j.rest.RestTelegramClient;
import telegram4j.rest.RouterResources;

public final class TelegramClient {

    private final String token;
    private final RestResources restResources;
    private final RestTelegramClient restClient;
    private final ClientResources clientResources;

    TelegramClient(String token, RestResources restResources, ClientResources clientResources) {
        this.token = token;
        this.restResources = restResources;
        this.restClient = new RestTelegramClient(new DefaultRouter(
                new RouterResources(token, restResources)));
        this.clientResources = clientResources;
    }

    public static TelegramClient create(String token) {
        return builder().setToken(token).build();
    }

    public static TelegramClientBuilder builder() {
        return new TelegramClientBuilder();
    }

    public RestResources getRestResources() {
        return restResources;
    }

    public RestTelegramClient getRestClient() {
        return restClient;
    }

    public Mono<Void> login() {
        // Currently nothing
        return Mono.empty();
    }
}
