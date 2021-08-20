package telegram4j;

import telegram4j.rest.RestResources;

import java.util.Objects;

public class TelegramClientBuilder {

    private String token;
    private RestResources restResources;

    TelegramClientBuilder() {}

    public TelegramClientBuilder setToken(String token) {
        this.token = Objects.requireNonNull(token, "token");
        return this;
    }

    public TelegramClientBuilder setRestResources(RestResources restResources) {
        this.restResources = Objects.requireNonNull(restResources, "restResources");
        return this;
    }

    public TelegramClient build() {
        RestResources restResources = getRestResources();
        return new TelegramClient(token, restResources);
    }

    private RestResources getRestResources() {
        if (restResources != null) {
            return restResources;
        }
        return new RestResources();
    }
}
