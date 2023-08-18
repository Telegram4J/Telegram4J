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
package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputPeer;

import java.util.Optional;

/** Type of {@code ProfilePhotoContext} which represents a profile photo received from message. */
public final class ChatPhotoContext extends ProfilePhotoContext {
    private final int messageId; // can be -1 for absent values

    ChatPhotoContext(InputPeer peer, int messageId) {
        super(peer);
        this.messageId = messageId;
    }

    @Override
    public Type getType() {
        return Type.CHAT_PHOTO;
    }

    public Optional<Integer> getMessageId() {
        return messageId == -1 ? Optional.empty() : Optional.of(messageId);
    }

    @Override
    void serialize(ByteBuf buf) {
        super.serialize(buf);
        if (messageId != -1) {
            buf.writeByte(1);
            buf.writeIntLE(messageId);
        } else {
            buf.writeByte(0);
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ChatPhotoContext that = (ChatPhotoContext) o;
        return messageId == that.messageId;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + messageId;
        h += (h << 5) + peer.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "ChatPhotoContext{" +
                "messageId=" + messageId +
                ", peer=" + peer +
                '}';
    }
}
