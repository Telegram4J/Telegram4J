package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.util.annotation.Nullable;

/** An MTProto TCP transport wrapping.  */
public interface Transport {

    /** Quick acknowledgement bit-mask, which can be applied to the wrapped payload size. */
    int QUICK_ACK_MASK = 1 << 31;

    /**
     * Allocate buffer and write transport identifier.
     *
     * @param allocator The {@link ByteBufAllocator} to allocate {@link ByteBuf}.
     * @return The allocated by the given {@link ByteBufAllocator}
     * buffer with transport identifier.
     */
    ByteBuf identifier(ByteBufAllocator allocator);

    /**
     * Gets wrapped payload.
     *
     * @param payload The original buffer payload.
     * @param quickAck The state of quick ack.
     * @return The wrapped payload.
     */
    ByteBuf encode(ByteBuf payload, boolean quickAck);

    /**
     * Gets unwrapped payload if readable.
     *
     * @param payload The wrapped buffer payload.
     * @return The unwrapped payload if readable.
     */
    @Nullable
    ByteBuf decode(ByteBuf payload);

    /**
     * Checks slice of stream and returns {@literal true} if part completes byte stream.
     *
     * @param buf The part of byte stream.
     * @return The {@literal true} if buffer part completes stream.
     */
    boolean canDecode(ByteBuf buf);

    /**
     * Gets current quick acknowledgment mode state.
     *
     * @return The {@literal true} if quick ack is enabled, otherwise {@literal false}.
     */
    boolean supportQuickAck();

    /**
     * Toggle quick acknowledgment mode.
     *
     * @param enable The new state.
     */
    void setQuickAckState(boolean enable);
}
