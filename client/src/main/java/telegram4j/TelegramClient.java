package telegram4j;

import reactor.core.publisher.Mono;
import telegram4j.rest.RestResources;

public final class TelegramClient {

    private final String token;
    private final RestResources restResources;
    private final ClientResources clientResources;

    TelegramClient(String token, RestResources restResources, ClientResources clientResources) {
        this.token = token;
        this.restResources = restResources;
        this.clientResources = clientResources;
    }

    public static TelegramClient create(String token) {
        return builder().setToken(token).build();
    }

    public static TelegramClientBuilder builder() {
        return new TelegramClientBuilder();
    }

    public Mono<Void> login() {
        // Currently nothing
        return Mono.empty();
    }
}
