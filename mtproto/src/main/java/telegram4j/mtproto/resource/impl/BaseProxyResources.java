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
    public final Duration connectTimeout;
    public final InetSocketAddress address;

    protected BaseProxyResources(@Nullable Duration connectTimeout, InetSocketAddress address) {
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

    public abstract ProxyHandler createProxyHandler(SocketAddress resolvedAddress);

    public static abstract sealed class Spec implements AddressSpec, ProxySpec
            permits HttpProxyResourcesImpl.Spec, Sock5ProxyResourcesImpl.Spec {

        public InetSocketAddress address;
        @Nullable
        public Duration connectTimeout;

        @Override
        public AddressSpec from(ProxyResources proxyResources) {
            if (proxyResources instanceof BaseProxyResources b) {
                connectTimeout = b.connectTimeout;
                address = b.address;
            }
            return this;
        }

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
        public abstract ProxyResources build();
    }
}
