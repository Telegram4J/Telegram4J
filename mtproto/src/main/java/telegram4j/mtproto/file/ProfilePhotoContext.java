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

public sealed class ProfilePhotoContext extends Context
    permits ChatPhotoContext {

    protected final InputPeer peer;

    ProfilePhotoContext(InputPeer peer) {
        this.peer = peer;
    }

    @Override
    public Type getType() {
        return Type.PROFILE_PHOTO;
    }

    /**
     * Gets peer to which profile photo associated.
     *
     * <p> Returned {@code InputPeer} may contain access hash which valid
     * only for downloading profile photo, do not use this peer in other requests.
     *
     * @return The peer of profile photo.
     */
    public InputPeer getPeer() {
        return peer;
    }

    @Override
    void serialize(ByteBuf buf) {
        serializeInputPeer(buf, peer);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProfilePhotoContext that = (ProfilePhotoContext) o;
        return peer.equals(that.peer);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + peer.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "ProfilePhotoContext{" +
                "peer=" + peer +
                '}';
    }
}
