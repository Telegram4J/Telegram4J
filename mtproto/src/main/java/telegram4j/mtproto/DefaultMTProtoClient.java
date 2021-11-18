package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.FutureMono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.crypto.MTProtoAuthorizationContext;
import telegram4j.mtproto.crypto.MTProtoAuthorizationHandler;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

public class DefaultMTProtoClient implements MTProtoClient {

    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);

    private final MTProtoResources mtProtoResources;
    private final ConcurrentMap<DataCenter, MTProtoConnection> connections = new ConcurrentHashMap<>();
    private final MTProtoAuthorizationHandler authorizationHandler;
    private volatile MTProtoConnection currentConnection;

    public DefaultMTProtoClient(MTProtoResources mtProtoResources) {
        this.mtProtoResources = mtProtoResources;
        this.authorizationHandler = new MTProtoAuthorizationHandler(this, new MTProtoAuthorizationContext());
    }

    public MTProtoAuthorizationHandler getAuthorizationHandler() {
        return authorizationHandler;
    }

    @Override
    public Mono<Void> connect() {
        return connect0().then();
    }

    private Mono<MTProtoConnection> connect0() {
        return Mono.defer(() -> {
            if (currentConnection != null && currentConnection.getConnection().channel().isActive()) {
                return Mono.just(currentConnection);
            }

            List<DataCenter> dataCenters = mtProtoResources.isTest()
                    ? DataCenter.testDataCenters
                    : DataCenter.productionDataCenters;

            return Flux.fromIterable(dataCenters)
                    .flatMap(dc -> Mono.fromSupplier(() -> connections.get(dc))
                            .switchIfEmpty(createConnection(dc)))
                    .next()
                    .doOnNext(con -> currentConnection = con);
        });
    }

    private Mono<MTProtoConnection> createConnection(DataCenter dc) {
        return Mono.defer(() -> {
            Sinks.Many<ByteBuf> inboundSink = newEmitterSink();

            return mtProtoResources.getTcpClient()
                    .remoteAddress(() -> new InetSocketAddress(dc.getAddress(), dc.getPort()))
                    .handle((inbound, outbound) -> inbound.receive()
                            .map(ByteBuf::retain)
                            .doOnNext(buf -> inboundSink.emitNext(buf, FAIL_FAST))
                            .then())
                    .connect()
                    .doOnNext(con -> con.addHandler(new MTProtoIdentifierChannelInitializer()))
                    .map(con -> new MTProtoConnection(con, inboundSink, dc))
                    .doOnNext(con -> {
                        log.debug("Connected to datacenter #{}", con.getDataCenter().getId());
                        connections.put(con.getDataCenter(), con);
                    });
        });
    }

    private static <T> Sinks.Many<T> newEmitterSink() {
        return Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    @Override
    public Mono<Void> send(ByteBuf payload) {
        return connect0().flatMap(con -> {
                    Channel channel = con.getConnection().channel();
                    return FutureMono.from(channel.writeAndFlush(
                            mtProtoResources.getTransport()
                                    .encode(channel.alloc(), payload)));
                })
                .then();
    }

    @Override
    public Flux<ByteBuf> receiver() {
        return connect0().flatMapMany(con -> con.getReceiver().asFlux()
                .map(ByteBuf::retainedDuplicate)
                .map(buf -> mtProtoResources.getTransport()
                        .decode(con.getConnection()
                                .channel().alloc(), buf))
                .flatMap(buf -> {
                    if (buf.readableBytes() == Integer.BYTES) { // The error code writes as negative int32
                        int code = buf.readIntLE() * -1;
                        return Mono.error(() -> TransportException.create(code));
                    }
                    return Mono.just(buf);
                })
                .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease));
    }

    @Override
    public Mono<Void> onDispose() {
        return connect0().flatMap(con -> con.getConnection().onDispose());
    }

    private class MTProtoIdentifierChannelInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            log.debug("Sending header identifier to the server.");

            ByteBuf identifier = mtProtoResources.getTransport()
                    .identifier(ch.alloc());

            ch.writeAndFlush(identifier);
        }
    }
}
