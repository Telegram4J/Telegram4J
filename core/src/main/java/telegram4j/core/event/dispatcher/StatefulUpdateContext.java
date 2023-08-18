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
package telegram4j.core.event.dispatcher;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.tl.Update;

import java.util.Map;

public class StatefulUpdateContext<U extends Update, O> extends UpdateContext<U> {
    @Nullable
    private final O old;

    protected StatefulUpdateContext(MTProtoTelegramClient client, Map<Id, Chat> chats,
                                    Map<Id, User> users, U update, @Nullable O old) {
        super(client, chats, users, update);
        this.old = old;
    }

    public static <U extends Update, O> StatefulUpdateContext<U, O> from(UpdateContext<U> context, @Nullable O old) {
        return new StatefulUpdateContext<>(context.getClient(), context.getChats(),
                context.getUsers(), context.getUpdate(), old);
    }

    @Nullable
    public O getOld() {
        return old;
    }
}
