package telegram4j.mtproto.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import telegram4j.mtproto.MTProtoException;
import telegram4j.mtproto.TransportException;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.api.MTProtoObject;

final class HandshakeCodec extends ChannelDuplexHandler {

    private final AuthData authData;

    public HandshakeCodec(AuthData authData) {
        this.authData = authData;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!(msg instanceof MTProtoObject o)) {
            throw new IllegalStateException("Unexpected type of outbound");
        }

        ctx.write(encode(ctx, o), promise);
    }

    private ByteBuf encode(ChannelHandlerContext ctx, MTProtoObject msg) {
        int size = TlSerializer.sizeOf(msg);
        ByteBuf payload = ctx.alloc().buffer(20 + size)
                .writeLongLE(0) // auth key id
                .writeLongLE(authData.nextMessageId()) // Message id in the auth requests doesn't allow receiving payload
                .writeIntLE(size);
        TlSerializer.serialize(payload, msg);
        return payload;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof ByteBuf b)) {
            throw new IllegalStateException("Unexpected type of inbound: " + msg.getClass());
        }
        if (b.readableBytes() == 4) {
            int code = b.readIntLE();
            b.release();

            throw new TransportException(code);
        }

        MTProtoObject decoded;
        try {
            decoded = decode(b);
        } finally {
            b.release();
        }

        ctx.fireChannelRead(decoded);
    }

    private MTProtoObject decode(ByteBuf buf) {
        long authKeyId = buf.readLongLE();
        if (authKeyId != 0) {
            throw new MTProtoException("Received message with non-zero authKeyId");
        }

        buf.readLongLE(); // messageId
        int payloadLength = buf.readIntLE();
        ByteBuf payload = buf.readSlice(payloadLength);
        if (buf.isReadable()) {
            throw new MTProtoException("Received packet with incorrect payloadLength");
        }

        return TlDeserializer.deserialize(payload);
    }
}
