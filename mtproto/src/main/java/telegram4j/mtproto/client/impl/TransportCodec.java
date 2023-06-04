package telegram4j.mtproto.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import telegram4j.mtproto.transport.Transport;

import java.util.List;
import java.util.Objects;

public final class TransportCodec extends ChannelDuplexHandler {
    public static final AttributeKey<Boolean> quickAck = AttributeKey.valueOf("quickAck");

    private final Transport transport;
    private final Decoder decoder = new Decoder();
    private final Encoder encoder = new Encoder();

    public TransportCodec(Transport transport) {
        this.transport = Objects.requireNonNull(transport);
    }

    public Transport delegate() {
        return transport;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        decoder.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        encoder.write(ctx, msg, promise);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        decoder.channelReadComplete(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        decoder.channelInactive(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(quickAck).set(false);

        try {
            decoder.handlerAdded(ctx);
        } finally {
            encoder.handlerAdded(ctx);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(quickAck).set(null);

        try {
            decoder.handlerRemoved(ctx);
        } finally {
            encoder.handlerRemoved(ctx);
        }
    }

    class Decoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            ByteBuf res = transport.tryDecode(in);
            if (res != null) {
                out.add(res);
            }
        }
    }

    class Encoder extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            if (msg instanceof ByteBuf b) {
                boolean quickAck = ctx.channel().attr(TransportCodec.quickAck).get();
                ByteBuf encoded;
                try {
                    encoded = transport.encode(b, quickAck);
                } finally {
                    // b.release();
                    // b may already be released in Transport.encode(...)
                    // ReferenceCountUtil.safeRelease(b);
                    // System.out.println(b);
                }
                ctx.write(encoded, promise);
            } else {
                ctx.write(msg, promise);
            }
        }
    }
}
