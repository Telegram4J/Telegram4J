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
import telegram4j.tl.Peer;

public final class MessageMediaContext extends Context {
    private final Peer chatPeer;
    private final int messageId;

    MessageMediaContext(Peer chatPeer, int messageId) {
        this.chatPeer = chatPeer;
        this.messageId = messageId;
    }

    public Peer getChatPeer() {
        return chatPeer;
    }

    public int getMessageId() {
        return messageId;
    }

    @Override
    public Type getType() {
        return Type.MESSAGE_MEDIA;
    }

    @Override
    void serialize(ByteBuf buf) {
        serializePeer(buf, chatPeer);
        buf.writeIntLE(messageId);
    }

    @Override
    public String toString() {
        return "MessageMediaContext{" +
                "chatPeer=" + chatPeer +
                ", messageId=" + messageId +
                '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMediaContext that = (MessageMediaContext) o;
        return messageId == that.messageId && chatPeer.equals(that.chatPeer);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + chatPeer.hashCode();
        h += (h << 5) + messageId;
        return h;
    }
}
