package telegram4j.mtproto.resource.impl;

import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;

public final class Sock5ProxyResourcesImpl extends BaseProxyResources {

    private Sock5ProxyResourcesImpl(String username, String password,
                                    Duration connectTimeout, InetSocketAddress address) {
        super(username, password, connectTimeout, address);
    }

    @Override
    public ProxyHandler createProxyHandler(SocketAddress resolvedAddress) {
        var handler = new Socks5ProxyHandler(resolvedAddress, username, password);

        if (connectTimeout != null) {
            handler.setConnectTimeoutMillis(connectTimeout.toMillis());
        }
        return handler;
    }

    public static final class Spec extends BaseProxyResources.Spec {
        @Override
        public Sock5ProxyResourcesImpl build() {
            return new Sock5ProxyResourcesImpl(username, password, connectTimeout, address);
        }
    }
}
