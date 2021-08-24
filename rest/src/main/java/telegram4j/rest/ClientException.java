package telegram4j.rest;

import java.util.Objects;

public class ClientException extends RuntimeException {

    private static final long serialVersionUID = -7683981330231749926L;

    private final TelegramRequest request;
    private final RestClientResponse response;

    public ClientException(TelegramRequest request, RestClientResponse response, ErrorResponse errorResponse) {
        super(request.getRoute().getMethod() + " " + request.getRoute().getUri() +
                " returned " + errorResponse.getErrorCode() + errorResponse.getDescription()
                .map(s -> " with description: " + s).orElse(""));
        this.request = Objects.requireNonNull(request, "request");
        this.response = Objects.requireNonNull(response, "response");
    }

    public TelegramRequest getRequest() {
        return request;
    }

    public RestClientResponse getResponse() {
        return response;
    }
}
