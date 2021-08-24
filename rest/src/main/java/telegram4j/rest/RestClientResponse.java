package telegram4j.rest;

import reactor.netty.ByteBufMono;
import reactor.netty.NettyInbound;
import reactor.netty.http.client.HttpClientResponse;

public class RestClientResponse {

    private final HttpClientResponse httpResponse;
    private final NettyInbound inbound;

    RestClientResponse(HttpClientResponse httpResponse, NettyInbound inbound) {
        this.httpResponse = httpResponse;
        this.inbound = inbound;
    }

    public HttpClientResponse getHttpResponse() {
        return httpResponse;
    }

    public ByteBufMono getBody() {
        return inbound.receive()
                .aggregate();
    }
}
