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
import telegram4j.mtproto.resource.impl.Sock5ProxyResourcesImpl;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * A parameters of Socks 5 proxy.
 *
 * @implNote Implementation of this resource is immutable and thread-safe.
 */
public sealed interface SocksProxyResources extends ProxyResources permits Sock5ProxyResourcesImpl {

    /** {@return The username for proxy} if present. */
    Optional<String> username();

    /** {@return The password for proxy} if present. */
    Optional<String> password();

    /** A helper builder of mandatory {@code address} attribute. */
    sealed interface AddressSpec extends ProxyResources.AddressSpec permits Sock5ProxyResourcesImpl.Spec {

        @Override
        AddressSpec from(ProxyResources proxyResources);

        /**
         * Configures proxy address.
         *
         * @param address The proxy address, non-null.
         * @return The {@code ProxySpec}.
         */
        @Override
        ProxySpec address(InetSocketAddress address);
    }

    /** A helper builder of optional attributes. */
    sealed interface ProxySpec extends ProxyResources.ProxySpec permits Sock5ProxyResourcesImpl.Spec {
        @Override
        ProxySpec connectTimeout(@Nullable Duration connectTimeout);

        /**
         * Configures username for proxy.
         *
         * @throws IllegalArgumentException if length of username excess 256 characters.
         * @param username The username for proxy.
         * @return This spec.
         */
        ProxySpec username(@Nullable String username);

        /**
         * Configures password for proxy.
         *
         * @throws IllegalArgumentException if length of password excess 256 characters.
         * @param password The password for proxy.
         * @return This spec.
         */
        ProxySpec password(@Nullable String password);

        /**
         * Builds {@code SocksProxyResources} from this spec.
         *
         * @return new {@code SocksProxyResources}.
         */
        @Override
        SocksProxyResources build();
    }
}
