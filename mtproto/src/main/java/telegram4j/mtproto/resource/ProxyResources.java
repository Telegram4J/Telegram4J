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
        permits HttpProxyResources, BaseProxyResources {

    /**
     * Creates new spec builder of Socks 5 proxy.
     *
     * @apiNote Returning spec is not thread safe must be used locally or with synchronization.
     * @return A new spec of Socks 5 proxy.
     */
    static AddressSpec ofSocks5() {
        return new Sock5ProxyResourcesImpl.Spec();
    }

    /**
     * Creates new spec builder of HTTP proxy.
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

    /** {@return The username for proxy} if present. */
    Optional<String> username();

    /** {@return The password for proxy} if present. */
    Optional<String> password();

    /** A helper builder of mandatory {@code address} attribute. */
    sealed interface AddressSpec permits HttpProxyResources.AddressSpec, BaseProxyResources.Spec {

        /**
         * Configures proxy address.
         *
         * @param address The proxy address, non-null.
         * @return The {@code ProxySpec}.
         */
        ProxySpec address(InetSocketAddress address);
    }

    /** A helper builder of optional attributes. */
    sealed interface ProxySpec permits HttpProxyResources.ProxySpec, BaseProxyResources.Spec {
        /**
         * Configures initial HTTP headers for proxy.
         *
         * @param connectTimeout The connection timeout.
         * @return This spec.
         */
        ProxySpec connectTimeout(@Nullable Duration connectTimeout);

        /**
         * Configures username for proxy.
         *
         * @param username The username for proxy.
         * @return This spec.
         */
        ProxySpec username(@Nullable String username);

        /**
         * Configures password for proxy.
         *
         * @param password The password for proxy.
         * @return This spec.
         */
        ProxySpec password(@Nullable String password);

        /**
         * Builds {@code ProxyResources} from this spec.
         *
         * @return new {@code ProxyResources}.
         */
        ProxyResources build();
    }
}
