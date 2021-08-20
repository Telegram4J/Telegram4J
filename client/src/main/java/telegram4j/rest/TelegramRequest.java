package telegram4j.rest;

import reactor.util.annotation.Nullable;
import telegram4j.rest.route.Route;

import java.util.Objects;

public class TelegramRequest {

    private final Route route;

    @Nullable
    private Object body;

    public TelegramRequest(Route route) {
        this.route = Objects.requireNonNull(route, "route");
    }

    public Route getRoute() {
        return route;
    }

    @Nullable
    public Object getBody() {
        return body;
    }

    public TelegramRequest body(@Nullable Object body) {
        this.body = body;
        return this;
    }
}
