package telegram4j.mtproto.resource.impl;

import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.resource.ProxyResources;
import telegram4j.mtproto.resource.SocksProxyResources;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Optional;

public final class Sock5ProxyResourcesImpl extends BaseProxyResources implements SocksProxyResources {

    @Nullable
    public final String username;
    @Nullable
    public final String password;

    private Sock5ProxyResourcesImpl(@Nullable String username, @Nullable String password,
                                    @Nullable Duration connectTimeout, InetSocketAddress address) {
        super(connectTimeout, address);
        this.username = username;
        this.password = password;
    }

    @Override
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    @Override
    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    @Override
    public ProxyHandler createProxyHandler(SocketAddress resolvedAddress) {
        var handler = new Socks5ProxyHandler(resolvedAddress, username, password);

        if (connectTimeout != null) {
            handler.setConnectTimeoutMillis(connectTimeout.toMillis());
        }
        return handler;
    }

    public static final class Spec
            extends BaseProxyResources.Spec
            implements SocksProxyResources.AddressSpec, SocksProxyResources.ProxySpec {

        @Nullable
        public String username;
        @Nullable
        public String password;

        @Override
        public Spec from(ProxyResources proxyResources) {
            if (proxyResources instanceof Sock5ProxyResourcesImpl s) {
                username = s.username;
                password = s.password;
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
        public Spec username(@Nullable String username) {
            if (username != null && username.length() > 255) {
                throw new IllegalArgumentException("Username expected to be less than 256 chars");
            }
            this.username = username;
            return this;
        }

        @Override
        public Spec password(@Nullable String password) {
            if (password != null && password.length() > 255) {
                throw new IllegalArgumentException("Password expected to be less than 256 chars");
            }
            this.password = password;
            return this;
        }

        @Override
        public Sock5ProxyResourcesImpl build() {
            return new Sock5ProxyResourcesImpl(username, password, connectTimeout, address);
        }
    }
}
