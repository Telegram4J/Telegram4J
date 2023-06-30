package telegram4j.mtproto.resource.impl;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyHandler;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.resource.HttpProxyResources;
import telegram4j.mtproto.resource.ProxyResources;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Optional;

public final class HttpProxyResourcesImpl
        extends BaseProxyResources
        implements HttpProxyResources {
    @Nullable
    public final HttpHeaders httpHeaders;

    private HttpProxyResourcesImpl(@Nullable Duration connectTimeout, InetSocketAddress address,
                                   @Nullable HttpHeaders httpHeaders) {
        super(connectTimeout, address);
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Optional<HttpHeaders> httpHeaders() {
        return Optional.ofNullable(httpHeaders);
    }

    @Override
    public ProxyHandler createProxyHandler(SocketAddress resolvedAddress) {

        var handler = new HttpProxyHandler(resolvedAddress, httpHeaders);

        if (connectTimeout != null) {
            handler.setConnectTimeoutMillis(connectTimeout.toMillis());
        }

        return handler;
    }

    public static final class Spec
            extends BaseProxyResources.Spec
            implements HttpProxyResources.AddressSpec, HttpProxyResources.ProxySpec {

        @Nullable
        public HttpHeaders httpHeaders;

        @Override
        public Spec from(ProxyResources proxyResources) {
            if (proxyResources instanceof HttpProxyResourcesImpl h) {
                httpHeaders = h.httpHeaders;
            }

            return (Spec) super.from(proxyResources);
        }

        @Override
        public Spec address(InetSocketAddress address) {
            return (Spec) super.address(address);
        }

        @Override
        public Spec connectTimeout(@Nullable Duration connectTimeout) {
            return (Spec) super.connectTimeout(connectTimeout);
        }

        @Override
        public Spec httpHeaders(@Nullable HttpHeaders httpHeaders) {
            this.httpHeaders = httpHeaders;
            return this;
        }

        @Override
        public HttpProxyResourcesImpl build() {
            return new HttpProxyResourcesImpl(connectTimeout, address, httpHeaders);
        }
    }
}
