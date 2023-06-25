package telegram4j.mtproto.resource;

import io.netty.channel.EventLoopGroup;
import reactor.core.Disposable;
import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class TcpClientResources implements Disposable {
    @Nullable
    private final ProxyResources proxyResources;
    private final EventLoopResources eventLoopResources;
    private final EventLoopGroup eventLoopGroup;

    private TcpClientResources(@Nullable ProxyResources proxyResources,
                               EventLoopResources eventLoopResources,
                               EventLoopGroup eventLoopGroup) {
        this.proxyResources = proxyResources;
        this.eventLoopResources = eventLoopResources;
        this.eventLoopGroup = eventLoopGroup;
    }

    public Optional<ProxyResources> proxyProvider() {
        return Optional.ofNullable(proxyResources);
    }

    public EventLoopResources eventLoopResources() {
        return eventLoopResources;
    }

    public EventLoopGroup eventLoopGroup() {
        return eventLoopGroup;
    }

    public static TcpClientResources create() {
        return create(Transports.preferNative);
    }

    public static TcpClientResources create(boolean preferNative) {
        return builder()
                .eventLoopResources(EventLoopResources.create(preferNative))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void dispose() {
        eventLoopGroup.shutdownGracefully();
    }

    @Override
    public boolean isDisposed() {
        return eventLoopGroup.isShutdown();
    }

    public static class Builder {
        @Nullable
        ProxyResources proxyResources;
        @Nullable
        EventLoopResources eventLoopResources;

        private Builder() {}

        public Builder proxyResources(ProxyResources proxyResources) {
            this.proxyResources = Objects.requireNonNull(proxyResources);
            return this;
        }

        public Builder eventLoopResources(EventLoopResources eventLoopResources) {
            this.eventLoopResources = Objects.requireNonNull(eventLoopResources);
            return this;
        }

        public TcpClientResources build() {
            if (eventLoopResources == null) {
                eventLoopResources = EventLoopResources.create();
            }

            var eventLoopGroup = eventLoopResources.createEventLoopGroup();
            return new TcpClientResources(proxyResources, eventLoopResources, eventLoopGroup);
        }
    }
}
