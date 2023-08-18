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
package telegram4j.mtproto.resource;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.resource.impl.BaseProxyResources;
import telegram4j.mtproto.resource.impl.HttpProxyResourcesImpl;
import telegram4j.mtproto.resource.impl.Sock5ProxyResourcesImpl;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * A parameters of proxy connection.
 *
 * @implNote Implementation of this resource is immutable and thread-safe.
 */
public sealed interface ProxyResources
        permits HttpProxyResources, SocksProxyResources, BaseProxyResources {

    /**
     * Creates new spec builder of Socks 5 proxy.
     *
     * @apiNote Returning spec is not thread safe must be used locally or with synchronization.
     * @return A new spec of Socks 5 proxy.
     */
    static SocksProxyResources.AddressSpec ofSocks5() {
        return new Sock5ProxyResourcesImpl.Spec();
    }

    /**
     * Creates new spec builder of HTTP/HTTPS proxy.
     *
     * @apiNote Returning spec is not thread safe must be used locally or with synchronization.
     * @return A new spec builder of HTTP proxy.
     */
    static HttpProxyResources.AddressSpec ofHttp() {
        return new HttpProxyResourcesImpl.Spec();
    }

    /** {@return The address of proxy server} */
    InetSocketAddress address();

    /**
     * {@return The connect timeout} if the connection attempt to
     * the destination does not finish within the timeout,
     * the connection attempt will be failed. By default, is 10 seconds.
     */
    Optional<Duration> connectTimeout();

    /** A helper builder of mandatory {@code address} attribute. */
    sealed interface AddressSpec permits HttpProxyResources.AddressSpec, SocksProxyResources.AddressSpec, BaseProxyResources.Spec {

        /**
         * Configures this spec with values from specified.
         * <p>
         * If type of specified {@code proxyResources} is different
         * from type of the constructing resources, then
         * extra properties will be ignored
         *
         * @param proxyResources The base resources for modifying.
         * @return The spec, for chaining.
         */
        AddressSpec from(ProxyResources proxyResources);

        /**
         * Configures proxy address.
         *
         * @param address The proxy address, non-null.
         * @return The {@code ProxySpec}.
         */
        ProxySpec address(InetSocketAddress address);
    }

    /** A helper builder of optional attributes. */
    sealed interface ProxySpec permits HttpProxyResources.ProxySpec, SocksProxyResources.ProxySpec, BaseProxyResources.Spec {
        /**
         * Configures initial HTTP headers for proxy.
         *
         * @param connectTimeout The connection timeout.
         * @return This spec.
         */
        ProxySpec connectTimeout(@Nullable Duration connectTimeout);

        /**
         * Builds {@code ProxyResources} from this spec.
         *
         * @return new {@code ProxyResources}.
         */
        ProxyResources build();
    }
}
