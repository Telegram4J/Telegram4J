package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.ChatParticipantUpdateEvent;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.ChatParticipant;
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

        if (!context.getChats().containsKey(upd.channelId())) {
            return Flux.empty();
        }

        Channel channel = (Channel) Objects.requireNonNull(context.getChats().get(upd.channelId()));
        // I can't be sure that ChatParticipant have user information attached because that field named as userId :/
        User peer = Objects.requireNonNull(context.getUsers().get(upd.userId()));
        User actor = Objects.requireNonNull(context.getUsers().get(upd.actorId()));
        ExportedChatInvite invite = Optional.ofNullable(upd.invite())
                .map(e -> TlEntityUtil.unmapEmpty(e, ChatInviteExported.class))
                .map(d -> {
                    User admin = Objects.requireNonNull(context.getUsers().get(d.adminId()));

                    return new ExportedChatInvite(context.getClient(), d, admin);
                })
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
