package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public interface MTProtoClient {

    Mono<Void> connect();

    Mono<Void> send(ByteBuf payload);

    Sinks.Many<ByteBuf> receiver();

    Mono<Void> onDispose();
}
