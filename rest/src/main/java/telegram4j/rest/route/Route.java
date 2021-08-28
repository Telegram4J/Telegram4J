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

    private final String uri;
    private final HttpMethod method;

    private Route(String uri, HttpMethod method) {
        this.uri = Objects.requireNonNull(uri, "uri");
        this.method = Objects.requireNonNull(method, "method");
    }

    /**
     * Creates a new {@link Route} with {@link HttpMethod#GET} method.
     *
     * @param uri the URI string.
     * @return a new {@link Route}.
     */
    public static Route get(String uri) {
        return new Route(uri, HttpMethod.GET);
    }

    /**
     * Creates a new {@link Route} with {@link HttpMethod#POST} method.
     *
     * @param uri the URI string.
     * @return a new {@link Route}.
     */
    public static Route post(String uri) {
        return new Route(uri, HttpMethod.POST);
    }

    /**
     * Creates a new {@link Route} with custom {@link HttpMethod}.
     *
     * @param uri the URI string.
     * @param httpMethod the http method.
     * @return a new {@link Route}.
     */
    public static Route ofMethod(String uri, HttpMethod httpMethod) {
        return new Route(uri, httpMethod);
    }

    /**
     * Gets a URI string of this route.
     *
     * @return a URI string.
     */
    public String getUri() {
        return uri;
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
