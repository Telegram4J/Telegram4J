package telegram4j.mtproto.resource.impl;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.resource.HttpProxyResources;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Optional;

public final class HttpProxyResourcesImpl
        extends BaseProxyResources
        implements HttpProxyResources {

    public final HttpHeaders httpHeaders;

    private HttpProxyResourcesImpl(String username, String password,
                                   Duration connectTimeout, InetSocketAddress address,
                                   HttpHeaders httpHeaders) {
        super(username, password, connectTimeout, address);
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Optional<HttpHeaders> httpHeaders() {
        return Optional.ofNullable(httpHeaders);
    }

    @Override
    public ProxyHandler createProxyHandler(SocketAddress resolvedAddress) {

        var handler = username != null && password != null
                ? new HttpProxyHandler(resolvedAddress, username, password, httpHeaders)
                : new HttpProxyHandler(resolvedAddress, httpHeaders);

        if (connectTimeout != null) {
            handler.setConnectTimeoutMillis(connectTimeout.toMillis());
        }

        return handler;
    }

    public static final class Spec
            extends BaseProxyResources.Spec
            implements HttpProxyResources.AddressSpec, HttpProxyResources.ProxySpec {

        public HttpHeaders httpHeaders;

        @Override
        public Spec address(InetSocketAddress address) {
            return (Spec) super.address(address);
        }

        @Override
        public Spec connectTimeout(@Nullable Duration connectTimeout) {
            return (Spec) super.connectTimeout(connectTimeout);
        }

        @Override
        public Spec username(@Nullable String username) {
            return (Spec) super.username(username);
        }

        @Override
        public Spec password(@Nullable String password) {
            return (Spec) super.password(password);
        }

        @Override
        public Spec httpHeaders(HttpHeaders httpHeaders) {
            this.httpHeaders = httpHeaders;
            return this;
        }

        @Override
        public HttpProxyResourcesImpl build() {
            if (username == null ^ password == null) {
                throw new IllegalArgumentException("The password and username must be specified, or none");
            }
            return new HttpProxyResourcesImpl(username, password, connectTimeout, address, httpHeaders);
        }
    }
}
