package telegram4j.rest;

import java.util.Objects;

public class RouterResources {

    private final String token;
    private final RestResources restResources;

    public RouterResources(String token, RestResources restResources) {
        this.token = Objects.requireNonNull(token, "token");
        this.restResources = Objects.requireNonNull(restResources, "restResources");
    }

    public String getToken() {
        return token;
    }

    public RestResources getRestResources() {
        return restResources;
    }
}
