/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.util.annotation.Nullable;

/**
 * An MTProto TCP transport wrapping.
 *
 * <p> This class is only used in one thread
 * and no any synchronization is required.
 */
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
     * This method should not release incoming {@link ByteBuf}.
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
     * @return The unwrapped slice of payload if readable or {@code null} for cases when more bytes are needed.
     */
    @Nullable
    ByteBuf tryDecode(ByteBuf payload);

    /**
     * Gets current quick acknowledgment mode state.
     *
     * @return The {@literal true} if quick ack is enabled, otherwise {@code false}.
     */
    boolean supportsQuickAck();
}
