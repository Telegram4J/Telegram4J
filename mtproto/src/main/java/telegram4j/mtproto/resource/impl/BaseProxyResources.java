package telegram4j.mtproto.resource.impl;

import io.netty.handler.proxy.ProxyHandler;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.resource.ProxyResources;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public abstract sealed class BaseProxyResources
        implements ProxyResources
        permits HttpProxyResourcesImpl, Sock5ProxyResourcesImpl {

    @Nullable
    public final String username;
    @Nullable
    public final String password;
    @Nullable
    public final Duration connectTimeout;
    public final InetSocketAddress address;

    protected BaseProxyResources(String username, String password,
                                 Duration connectTimeout, InetSocketAddress address) {
        this.username = username;
        this.password = password;
        this.connectTimeout = connectTimeout;
        this.address = address;
    }

    @Override
    public InetSocketAddress address() {
        return address;
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.ofNullable(connectTimeout);
    }

    @Override
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    @Override
    public Optional<String> password() {
        return Optional.ofNullable(password);
    }

    public abstract ProxyHandler createProxyHandler(SocketAddress resolvedAddress);

    public static abstract sealed class Spec implements AddressSpec, ProxySpec
            permits HttpProxyResourcesImpl.Spec, Sock5ProxyResourcesImpl.Spec {

        public InetSocketAddress address;
        @Nullable
        public String username;
        @Nullable
        public String password;
        @Nullable
        public Duration connectTimeout;

        @Override
        public ProxySpec address(InetSocketAddress address) {
            this.address = Objects.requireNonNull(address);
            return this;
        }

        @Override
        public ProxySpec connectTimeout(@Nullable Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        @Override
        public ProxySpec username(@Nullable String username) {
            this.username = username;
            return this;
        }

        @Override
        public ProxySpec password(@Nullable String password) {
            this.password = password;
            return this;
        }

        @Override
        public abstract ProxyResources build();
    }
}
