package telegram4j.mtproto.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import telegram4j.mtproto.transport.Transport;

import java.util.List;

final class TransportCodec extends ChannelDuplexHandler {
    private final Transport transport;
    private final Decoder decoder = new Decoder();
    private final Encoder encoder = new Encoder();

    private boolean quickAck;

    public TransportCodec(Transport transport) {
        this.transport = transport;
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
        try {
            decoder.handlerAdded(ctx);
        } finally {
            encoder.handlerAdded(ctx);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        try {
            decoder.handlerRemoved(ctx);
        } finally {
            encoder.handlerRemoved(ctx);
        }
    }

    public void setQuickAck(boolean quickAck) {
        this.quickAck = quickAck;
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
            if (!(msg instanceof ByteBuf b)) {
                throw new IllegalStateException("Unexpected type of message: " + msg);
            }

            ByteBuf encoded;
            try {
                encoded = transport.encode(b, quickAck);
            } finally {
                // b.release();
                // b may already be released in Transport.encode(...)
                // ReferenceCountUtil.safeRelease(b);
            }
            ctx.write(encoded, promise);
        }
    }
}
