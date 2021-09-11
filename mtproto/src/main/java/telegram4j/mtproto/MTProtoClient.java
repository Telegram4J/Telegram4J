package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MTProtoClient {

    Mono<Void> connect();

    Mono<Void> send(ByteBuf payload);

    Flux<ByteBuf> receiver();

    Mono<Void> onDispose();
}
