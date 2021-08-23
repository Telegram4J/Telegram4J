package telegram4j.rest.route;

import io.netty.handler.codec.http.HttpMethod;
import reactor.util.annotation.Nullable;
import telegram4j.rest.TelegramRequest;

import java.util.Objects;

/**
 * Represents a tuple containing {@link HttpMethod} and URI used in
 * REST-services for requests creation.
 */
public class Route {

    private final String uriTemplate;
    private final HttpMethod method;

    private Route(String uriTemplate, HttpMethod method) {
        this.uriTemplate = uriTemplate;
        this.method = method;
    }

    /**
     * Creates a new {@link Route} with {@link HttpMethod#GET} method.
     *
     * @param uriTemplate the URI string.
     * @return a new {@link Route}.
     */
    public static Route get(String uriTemplate) {
        return new Route(uriTemplate, HttpMethod.GET);
    }

    /**
     * Creates a new {@link Route} with {@link HttpMethod#POST} method.
     *
     * @param uriTemplate the URI string.
     * @return a new {@link Route}.
     */
    public static Route post(String uriTemplate) {
        return new Route(uriTemplate, HttpMethod.POST);
    }

    /**
     * Creates a new {@link Route} with custom {@link HttpMethod}.
     *
     * @param uriTemplate the URI string.
     * @param httpMethod the http method.
     * @return a new {@link Route}.
     */
    public static Route ofMethod(String uriTemplate, HttpMethod httpMethod) {
        return new Route(uriTemplate, httpMethod);
    }

    /**
     * Gets a URI string of this route.
     *
     * @return a URI string.
     */
    public String getUriTemplate() {
        return uriTemplate;
    }

    /**
     * Gets a http method of this route.
     *
     * @return a {@link HttpMethod}.
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Constructs a new {@link TelegramRequest} from this route.
     *
     * @return a new {@link TelegramRequest}.
     */
    public TelegramRequest newRequest() {
        return new TelegramRequest(this);
    }

    @Override
    public boolean equals(@Nullable Object o) {
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
