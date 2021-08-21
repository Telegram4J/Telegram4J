package telegram4j.rest.route;

import io.netty.handler.codec.http.HttpMethod;
import telegram4j.rest.TelegramRequest;

import java.util.Objects;

public class Route {

    private final String uriTemplate;
    private final HttpMethod method;

    private Route(String uriTemplate, HttpMethod method) {
        this.uriTemplate = uriTemplate;
        this.method = method;
    }

    public static Route get(String uriTemplate) {
        return new Route(uriTemplate, HttpMethod.GET);
    }

    public static Route post(String uriTemplate) {
        return new Route(uriTemplate, HttpMethod.POST);
    }

    public static Route ofMethod(String uriTemplate, HttpMethod httpMethod) {
        return new Route(uriTemplate, httpMethod);
    }

    public String getUriTemplate() {
        return uriTemplate;
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
        return uriTemplate.equals(route.uriTemplate) && method.equals(route.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uriTemplate, method);
    }

    @Override
    public String toString() {
        return "Route{" +
                "uriTemplate='" + uriTemplate + '\'' +
                ", method=" + method +
                '}';
    }
}
