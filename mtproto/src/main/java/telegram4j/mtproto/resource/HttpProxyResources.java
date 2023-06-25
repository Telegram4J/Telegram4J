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

    /** {@return The username for proxy} if present then and {@link #password()} will be present too. */
    @Override
    Optional<String> username();

    /** {@return The password for proxy} if present then and {@link #username()} will be present too. */
    @Override
    Optional<String> password();

    sealed interface AddressSpec extends ProxyResources.AddressSpec permits HttpProxyResourcesImpl.Spec {
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

        @Override
        ProxySpec username(@Nullable String username);

        @Override
        ProxySpec password(@Nullable String password);

        /**
         * Builds {@code HttpProxyResources} from this spec.
         *
         * @return new {@code HttpProxyResources}.
         */
        @Override
        HttpProxyResources build();
    }
}
