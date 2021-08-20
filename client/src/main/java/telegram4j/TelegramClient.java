package telegram4j;

import telegram4j.rest.RestResources;

import java.util.Objects;

public final class TelegramClient {

    private final String token;
    private final RestResources restResources;

    TelegramClient(String token, RestResources restResources) {
        this.token = Objects.requireNonNull(token, "token");
        this.restResources = Objects.requireNonNull(restResources, "restResources");
    }

    public static TelegramClient create(String token) {
        return builder().setToken(token).build();
    }

    public static TelegramClientBuilder builder() {
        return new TelegramClientBuilder();
    }
}
