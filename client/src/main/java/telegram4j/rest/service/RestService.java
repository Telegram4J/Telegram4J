package telegram4j.rest.service;

import telegram4j.rest.RestRouter;

import java.util.Objects;

public abstract class RestService {

    protected final RestRouter router;

    protected RestService(RestRouter router) {
        this.router = Objects.requireNonNull(router, "router");
    }

    public RestRouter getRouter() {
        return router;
    }
}
