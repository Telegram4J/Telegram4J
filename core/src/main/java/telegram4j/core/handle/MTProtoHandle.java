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
package telegram4j.core.handle;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.InputPeer;
import telegram4j.tl.InputUser;
import telegram4j.tl.Peer;

public abstract class MTProtoHandle extends EntityHandle {

    protected MTProtoHandle(MTProtoTelegramClient client) {
        super(client);
    }

    protected long rawIdOf(InputUser inputUser) {
        return Id.of(inputUser, client.getSelfId()).asLong();
    }

    protected Peer toPeer(InputPeer inputPeer) {
        return Id.of(inputPeer, client.getSelfId()).asPeer();
    }
}
