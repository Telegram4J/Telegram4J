package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.FutureMono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

public class DefaultMTProtoClient implements MTProtoClient {

    private static final Logger log = Loggers.getLogger(DefaultMTProtoClient.class);

    private final MTProtoResources mtProtoResources;
    private final Map<DataCenter, MTProtoConnection> connections = new ConcurrentHashMap<>();
    private volatile MTProtoConnection currentConnection;

    public DefaultMTProtoClient(MTProtoResources mtProtoResources) {
        this.mtProtoResources = mtProtoResources;
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
                            .switchIfEmpty(mtProtoResources.getTcpClient()
                                    .host(dc.getAddress())
                                    .port(dc.getPort())
                                    .connect()
                                    .map(con -> new MTProtoConnection(con, dc))
                                    .doOnNext(con -> con.getConnection()
                                            .addHandler(new MTProtoIdentifierChannelInitializer()))))
                    .next()
                    .doOnNext(con -> {
                        log.info("Connected to datacenter #{}", con.getDataCenter().getId());
                        connections.put(con.getDataCenter(), con);
                        currentConnection = con;
                    })
                    .flatMap(con -> Mono.create(sink -> {
                        Disposable.Composite forCleanup = Disposables.composite();

                        forCleanup.add(con.getConnection().inbound()
                                .receive()
                                .map(ByteBuf::retain)
                                .doOnNext(buf -> con.getReceiver().emitNext(buf, FAIL_FAST))
                                .subscribe());

                        sink.success(con);

                        sink.onCancel(forCleanup);
                    }));
        });
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
                .doOnDiscard(ByteBuf.class, ReferenceCountUtil::safeRelease));
    }

    @Override
    public Mono<Void> onDispose() {
        return connect0().flatMap(con -> con.getConnection().onDispose());
    }

    private class MTProtoIdentifierChannelInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            log.info("Sending header identifier to the server.");

            ByteBuf identifier = mtProtoResources.getTransport()
                    .identifier(ch.alloc());

            ch.writeAndFlush(identifier);
        }
    }
}
