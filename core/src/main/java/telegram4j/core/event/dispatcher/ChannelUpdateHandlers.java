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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.ChatParticipantUpdateEvent;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.ChannelPeer;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.ExportedChatInvite;
import telegram4j.core.util.Id;
import telegram4j.tl.ChatInviteExported;
import telegram4j.tl.ChatInvitePublicJoinRequests;
import telegram4j.tl.UpdateChannelParticipant;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

class ChannelUpdateHandlers {
    // Bots can receive UpdateChannel and maybe we should handle them

    // State handler
    // =====================

    static Mono<Void> handleStateUpdateChatParticipant(UpdateContext<UpdateChannelParticipant> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChannelParticipant(context.getUpdate());
    }

    // Update handler
    // =====================

    static Flux<ChatParticipantUpdateEvent> handleUpdateChannelParticipant(StatefulUpdateContext<UpdateChannelParticipant, Void> context) {
        UpdateChannelParticipant upd = context.getUpdate();

        Id channelId = Id.ofChannel(upd.channelId());

        var channel = (ChannelPeer) Objects.requireNonNull(context.getChats().get(channelId));
        // I can't be sure that ChatParticipant have user information attached because that field named as userId :/
        User peer = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.userId())));
        User actor = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.actorId())));
        ExportedChatInvite invite = upd.invite() instanceof ChatInviteExported d
                ? new ExportedChatInvite(context.getClient(), d)
                : null;
        boolean joinRequests = upd.invite() == ChatInvitePublicJoinRequests.instance();
        Instant timestamp = Instant.ofEpochSecond(upd.date());
        ChatParticipant oldParticipant = Optional.ofNullable(upd.prevParticipant())
                .map(d -> new ChatParticipant(context.getClient(), peer, d, channel.getId()))
                .orElse(null);
        ChatParticipant currentParticipant = Optional.ofNullable(upd.newParticipant())
                .map(d -> new ChatParticipant(context.getClient(), peer, d, channel.getId()))
                .orElse(null);

        return Flux.just(new ChatParticipantUpdateEvent(context.getClient(),
                timestamp, oldParticipant, currentParticipant,
                invite, joinRequests, channel, actor));
    }

}
