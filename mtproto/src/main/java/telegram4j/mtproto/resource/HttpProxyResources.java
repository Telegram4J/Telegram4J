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

import io.netty.handler.codec.http.HttpHeaders;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.resource.impl.HttpProxyResourcesImpl;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;

/**
 * A parameters of HTTP proxy.
 *
 * @implNote Implementation of this resource is immutable and thread-safe.
 */
public sealed interface HttpProxyResources extends ProxyResources permits HttpProxyResourcesImpl {

    /** {@return The initial HTTP headers} if present. */
    Optional<HttpHeaders> httpHeaders();

    sealed interface AddressSpec extends ProxyResources.AddressSpec permits HttpProxyResourcesImpl.Spec {
        @Override
        AddressSpec from(ProxyResources proxyResources);

        @Override
        ProxySpec address(InetSocketAddress address);
    }

    sealed interface ProxySpec extends ProxyResources.ProxySpec permits HttpProxyResourcesImpl.Spec {
        /**
         * Configures initial HTTP headers for proxy.
         *
         * @param httpHeaders The initial HTTP headers.
         * @return This spec.
         */
        ProxySpec httpHeaders(@Nullable HttpHeaders httpHeaders);

        @Override
        ProxySpec connectTimeout(@Nullable Duration connectTimeout);

        /**
         * Builds {@code HttpProxyResources} from this spec.
         *
         * @return new {@code HttpProxyResources}.
         */
        @Override
        HttpProxyResources build();
    }
}
