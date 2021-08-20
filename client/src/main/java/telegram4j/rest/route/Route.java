package telegram4j.rest.route;

import io.netty.handler.codec.http.HttpMethod;
import telegram4j.rest.TelegramRequest;

import java.util.Objects;

public class Route {

    private final String uri;
    private final HttpMethod method;

    private Route(String uri, HttpMethod method) {
        this.uri = Objects.requireNonNull(uri, "uri");
        this.method = Objects.requireNonNull(method, "method");
    }

    public static Route get(String uri) {
        return new Route(uri, HttpMethod.GET);
    }

    public static Route post(String uri) {
        return new Route(uri, HttpMethod.POST);
    }

    public static Route ofMethod(String uri, HttpMethod httpMethod) {
        return new Route(uri, httpMethod);
    }

    public String getUri() {
        return uri;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public TelegramRequest newRequest() {
        return new TelegramRequest(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Route route = (Route) o;
        return uri.equals(route.uri) && method.equals(route.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, method);
    }

    @Override
    public String toString() {
        return "Route{" +
                "uri='" + uri + '\'' +
                ", method=" + method +
                '}';
    }
}
