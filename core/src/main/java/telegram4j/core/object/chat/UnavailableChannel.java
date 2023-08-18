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

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.ChannelForbidden;

import java.time.Instant;
import java.util.Optional;

public final class UnavailableChannel extends BaseUnavailableChat implements ChannelPeer, UnavailableChat {
    private final ChannelForbidden data;

    public UnavailableChannel(MTProtoTelegramClient client, ChannelForbidden data) {
        super(client);
        this.data = data;
    }

    @Override
    public Id getId() {
        return Id.ofChannel(data.id(), data.accessHash());
    }

    @Override
    public Type getType() {
        return data.broadcast() ? Type.CHANNEL : Type.SUPERGROUP;
    }

    @Override
    public String getName() {
        return data.title();
    }

    public Optional<Instant> getUntilTimestamp() {
        return Optional.ofNullable(data.untilDate())
                .map(Instant::ofEpochSecond);
    }

    @Override
    public String toString() {
        return "UnavailableChannel{" +
                "data=" + data +
                '}';
    }
}
