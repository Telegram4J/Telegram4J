package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.concurrent.Queues;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

public class DefaultMTProtoClient implements MTProtoClient {

    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);

    private final MTProtoOptions options;
    private final ConcurrentMap<DataCenter, MTProtoSession> sessions = new ConcurrentHashMap<>();

    private volatile MTProtoSession currentSession;

    public DefaultMTProtoClient(MTProtoOptions options) {
        this.options = options;
    }

    @Override
    public Mono<MTProtoSession> openSession() {
        return Mono.defer(() -> {
            MTProtoSession currentSession = this.currentSession;
            if (currentSession != null && currentSession.getConnection().channel().isActive()) {
                return Mono.just(currentSession);
            }

            List<DataCenter> dataCenters = options.getResources().isTest()
                    ? DataCenter.testDataCenters
                    : DataCenter.productionDataCenters;

            return Mono.just(dataCenters.get(1))
                    .flatMap(dc -> Mono.justOrEmpty(sessions.get(dc))
                            .switchIfEmpty(createSession(dc)));
        });
    }

    @Override
    public Mono<MTProtoSession> getSession(DataCenter dc) {
        return Mono.fromSupplier(() -> sessions.get(dc));
    }

    @Override
    public MTProtoOptions getOptions() {
        return options;
    }

    private Mono<MTProtoSession> createSession(DataCenter dc) {
        return Mono.defer(() -> {
            Sinks.Many<ByteBuf> inboundSink = newEmitterSink();

            return options.getResources().getTcpClient()
                    .remoteAddress(() -> new InetSocketAddress(dc.getAddress(), dc.getPort()))
                    .doOnConnected(con -> {
                        log.debug("Connected to datacenter №{} ({}:{})", dc.getId(), dc.getAddress(), dc.getPort());
                        log.debug("Sending header identifier to the server.");

                        ByteBuf identifier = options.getResources().getTransport()
                                .identifier(con.channel().alloc());

                        con.channel().writeAndFlush(identifier);
                    })
                    .doOnDisconnected(con -> log.debug("Disconnected from the datacenter №{} ({}:{})",
                            dc.getId(), dc.getAddress(), dc.getPort()))
                    .handle((inbound, outbound) -> inbound.receive()
                            .map(ByteBuf::retain)
                            .doOnNext(buf -> inboundSink.emitNext(buf, FAIL_FAST))
                            .then())
                    .connect()
                    .map(con -> new MTProtoSession(this, con, inboundSink, dc))
                    .doOnNext(session -> {
                        currentSession = session;
                        sessions.put(dc, session);
                    });
        });
    }

    private static <T> Sinks.Many<T> newEmitterSink() {
        return Sinks.many().multicast().onBackpressureBuffer();
    }

    @Override
    public Mono<Void> close() {
        return Flux.fromIterable(sessions.values())
                .doOnNext(session -> session.getConnection().dispose())
                .then();
    }
}
