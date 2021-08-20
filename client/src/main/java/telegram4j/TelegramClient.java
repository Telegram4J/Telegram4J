package telegram4j;

import telegram4j.rest.DefaultRouter;
import telegram4j.rest.RestResources;
import telegram4j.rest.RestTelegramClient;
import telegram4j.rest.RouterResources;

import java.util.Objects;

public final class TelegramClient {

    private final String token;
    private final RestResources restResources;
    private final RestTelegramClient restClient;

    TelegramClient(String token, RestResources restResources) {
        this.token = Objects.requireNonNull(token, "token");
        this.restResources = Objects.requireNonNull(restResources, "restResources");
        this.restClient = new RestTelegramClient(new DefaultRouter(
                new RouterResources(token, restResources)));
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
}
