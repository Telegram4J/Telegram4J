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

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.BotInfo;
import telegram4j.core.util.Id;
import telegram4j.tl.BaseChatFull;
import telegram4j.tl.ChatForbidden;
import telegram4j.tl.ChatFull;

import java.util.List;

public final class UnavailableGroupChat extends BaseUnavailableChat implements GroupChatPeer, UnavailableChat {

    private final ChatForbidden data;

    public UnavailableGroupChat(MTProtoTelegramClient client, ChatForbidden data) {
        super(client);
        this.data = data;
    }

    @Override
    public Id getId() {
        return Id.ofChat(data.id());
    }

    @Override
    public Type getType() {
        return Type.GROUP;
    }

    @Override
    public String getName() {
        return data.title();
    }

    @Override
    public String toString() {
        return "UnavailableGroupChat{" +
                "data=" + data +
                '}';
    }
}
