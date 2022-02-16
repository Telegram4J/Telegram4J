package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.chat.ChannelParticipantUpdateEvent;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Id;
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

    static Flux<ChannelParticipantUpdateEvent> handleUpdateChannelParticipant(StatefulUpdateContext<UpdateChannelParticipant, Void> context) {
        UpdateChannelParticipant upd = context.getUpdate();

        Channel channel = Optional.ofNullable(context.getChats().get(upd.channelId()))
                .map(d -> (Channel) EntityFactory.createChat(context.getClient(), d, null))
                .orElseThrow();
        Id chatId = channel.getId();
        User user = Optional.ofNullable(context.getUsers().get(upd.userId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        User actor = Optional.ofNullable(context.getUsers().get(upd.actorId()))
                .map(d -> new User(context.getClient(), d))
                .orElseThrow();
        ExportedChatInvite invite = Optional.ofNullable(upd.invite())
                .map(d -> new ExportedChatInvite(context.getClient(), d))
                .orElse(null);
        Instant timestamp = Instant.ofEpochSecond(upd.date());
        ChatParticipant oldParticipant = Optional.ofNullable(upd.prevParticipant())
                .map(d -> new ChatParticipant(context.getClient(), d, chatId))
                .orElse(null);
        ChatParticipant currentParticipant = Optional.ofNullable(upd.newParticipant())
                .map(d -> new ChatParticipant(context.getClient(), d, chatId))
                .orElse(null);

        return Flux.just(new ChannelParticipantUpdateEvent(context.getClient(),
                channel, timestamp, actor, user, oldParticipant, currentParticipant,
                invite, upd.qts()));
    }

}
