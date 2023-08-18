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
package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.store.object.ResolvedDeletedMessages;
import telegram4j.tl.*;

public interface UpdatesStore {

    Mono<Void> onNewMessage(Message update);

    Mono<Message> onEditMessage(Message update);

    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessages update);
    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteScheduledMessages update);
    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteChannelMessages update);

    Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessages payload);
    Mono<Void> onUpdatePinnedMessages(UpdatePinnedChannelMessages payload);

    Mono<Void> onChatParticipant(UpdateChatParticipant payload);

    Mono<Void> onChannelParticipant(UpdateChannelParticipant payload);

    Mono<Void> onChatParticipants(ChatParticipants payload);
}
