/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
