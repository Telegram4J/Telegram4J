package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.ChatParticipantUpdateEvent;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.util.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.ChatInviteExported;
import telegram4j.tl.UpdateChannelParticipant;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

class ChannelUpdateHandlers {

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

        Id channelId = Id.ofChannel(upd.channelId(), null);
        if (!context.getChats().containsKey(channelId)) {
            return Flux.empty();
        }

        Channel channel = (Channel) Objects.requireNonNull(context.getChats().get(channelId));
        // I can't be sure that ChatParticipant have user information attached because that field named as userId :/
        User peer = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.userId(), null)));
        User actor = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.actorId(), null)));
        ExportedChatInvite invite = Optional.ofNullable(upd.invite())
                .map(e -> TlEntityUtil.unmapEmpty(e, ChatInviteExported.class))
                .map(d -> new ExportedChatInvite(context.getClient(), d, context.getUsers()
                        .get(Id.ofUser(d.adminId(), null))))
                .orElse(null);
        Instant timestamp = Instant.ofEpochSecond(upd.date());
        ChatParticipant oldParticipant = Optional.ofNullable(upd.prevParticipant())
                .map(d -> new ChatParticipant(context.getClient(), peer, d, channel.getId()))
                .orElse(null);
        ChatParticipant currentParticipant = Optional.ofNullable(upd.newParticipant())
                .map(d -> new ChatParticipant(context.getClient(), peer, d, channel.getId()))
                .orElse(null);

        return Flux.just(new ChatParticipantUpdateEvent(context.getClient(),
                timestamp, oldParticipant, currentParticipant,
                invite, upd.qts(), channel, actor));
    }

}
