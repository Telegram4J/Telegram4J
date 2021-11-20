package telegram4j.mtproto.crypto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import reactor.core.publisher.Mono;
import reactor.netty.FutureMono;
import telegram4j.mtproto.MTProtoSession;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlMethod;
import telegram4j.tl.TlObject;
import telegram4j.tl.TlSerializer;

class UnencryptedPayloadMapper implements PayloadMapper {

    private final MTProtoSession session;

    UnencryptedPayloadMapper(MTProtoSession session) {
        this.session = session;
    }

    @Override
    public <R, T extends TlMethod<R>> Mono<R> send(T object) {
        return Mono.defer(() -> {
            Channel channel = session.getConnection().channel();
            ByteBufAllocator alloc = channel.alloc();
            ByteBuf data = TlSerializer.serialize(alloc, object);
            long messageId = session.getMessageId();

            ByteBuf payload = alloc.buffer()
                    .writeLongLE(0) // auth key id
                    .writeLongLE(messageId)
                    .writeIntLE(data.readableBytes())
                    .writeBytes(data);

            return FutureMono.from(channel.writeAndFlush(
                            session.getClient().getOptions()
                                    .getResources().getTransport().encode(payload)))
                    .then(Mono.empty()); // TODO, implement callback
        });
    }

    @Override
    public <T extends TlObject> Mono<T> receive(ByteBuf payload) {
        return Mono.fromSupplier(() -> {
            long authKeyId = payload.readLongLE();
            if (authKeyId != 0) {
                throw new IllegalStateException("Auth key id must be zero, but received: " + authKeyId);
            }

            payload.skipBytes(12); // message id (8) + payload length (4)

            return TlDeserializer.deserialize(payload);
        });
    }
}
