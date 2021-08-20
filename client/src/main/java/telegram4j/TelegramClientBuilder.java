package telegram4j;

import telegram4j.rest.RestResources;

import java.util.Objects;

public class TelegramClientBuilder {

    private String token;
    private RestResources restResources;
    private ClientResources clientResources;

    TelegramClientBuilder() {}

    public TelegramClientBuilder setToken(String token) {
        this.token = Objects.requireNonNull(token, "token");
        return this;
    }

    public TelegramClientBuilder setRestResources(RestResources restResources) {
        this.restResources = Objects.requireNonNull(restResources, "restResources");
        return this;
    }

    public TelegramClientBuilder setClientResources(ClientResources clientResources) {
        this.clientResources = Objects.requireNonNull(clientResources, "clientResources");
        return this;
    }

    public TelegramClient build() {
        RestResources restResources = getRestResources();
        ClientResources clientResources = getClientResources();
        return new TelegramClient(token, restResources, clientResources);
    }

    private RestResources getRestResources() {
        if (restResources != null) {
            return restResources;
        }
        return new RestResources();
    }

    private ClientResources getClientResources() {
        if (clientResources != null) {
            return clientResources;
        }
        return new ClientResources();
    }
}
