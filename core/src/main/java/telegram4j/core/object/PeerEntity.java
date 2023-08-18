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
package telegram4j.core.object;

import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;

/** An object that can be an author or a chat from where messages are sent. */
public sealed interface PeerEntity extends TelegramObject permits MentionablePeer, Chat {

    /**
     * Gets the peer identifier of this entity.
     *
     * @return The peer identifier of this entity.
     */
    Id getId();

    /**
     * Checks if this peer is equal to the specified {@code PeerEntity}.
     * <p> The comparison is based on the {@link #getId()} and type checking.
     *
     * @param o The other peer, null returns {@code false}.
     * @return {@code true} if the other peer is equal to this one.
     */
    @Override
    boolean equals(Object o);

    /**
     * Returns a hash code value for this peer.
     * <p> The hash code is fully delegated to {@link Id#hashCode()} taken
     * from {@link #getId()}.
     *
     * @return A hash code value for this peer.
     */
    @Override
    int hashCode();
}
