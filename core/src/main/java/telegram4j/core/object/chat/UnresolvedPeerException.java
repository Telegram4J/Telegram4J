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
package telegram4j.core.object.chat;

import telegram4j.core.util.Id;
import telegram4j.tl.InputPeer;

import java.util.Objects;

/** Unchecked exception used to correctly process resolving of {@link InputPeer} object. */
public class UnresolvedPeerException extends RuntimeException {
    private final Id peerId;

    public UnresolvedPeerException(Id peerId) {
        super("Have no access to " + peerId + " peer" + peerId.getMinInformation()
                .map(Id.MinInformation::toString)
                .or(() -> peerId.getAccessHash().map(Object::toString))
                .map(s -> " with access info: " + s)
                .orElse("") +
                " or it's not present in local storage");
        this.peerId = Objects.requireNonNull(peerId);
    }

    /**
     * Gets id of peer which couldn't resolve in local storage.
     *
     * @return The id of peer which couldn't resolve in local storage.
     */
    public Id getPeerId() {
        return peerId;
    }
}
