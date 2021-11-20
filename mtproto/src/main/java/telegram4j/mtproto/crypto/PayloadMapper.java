package telegram4j.mtproto.crypto;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import telegram4j.tl.TlMethod;
import telegram4j.tl.TlObject;

public interface PayloadMapper {

    <R, T extends TlMethod<R>> Mono<R> send(T object);

    <T extends TlObject> Mono<T> receive(ByteBuf payload);
}
