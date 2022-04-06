package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.ChatParticipantUpdateEvent;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.util.EntityFactory;
import telegram4j.tl.UpdateChannelParticipant;

import java.time.Instant;
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

        // to resolve:
        // May be channel was unavailable and filtered out from chats map, just ignore event...
        // Or handle? But how map Chat object in these cases?
        if (!context.getChats().containsKey(upd.channelId())) {
            return Flux.empty();
        }

        Channel channel = Optional.ofNullable(context.getChats().get(upd.channelId()))
                .map(d -> (Channel) EntityFactory.createChat(context.getClient(), d, null))
                .orElseThrow();
        // I can't be sure that ChatParticipant have user information attached because that field named as userId :/
        User peer = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        User actor = Optional.ofNullable(context.getUsers().get(upd.actorId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        ExportedChatInvite invite = Optional.ofNullable(upd.invite())
                .map(d -> {
                    User admin = Optional.ofNullable(context.getUsers().get(d.adminId()))
                            .map(u -> EntityFactory.createUser(context.getClient(), u))
                            .orElseThrow();

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
