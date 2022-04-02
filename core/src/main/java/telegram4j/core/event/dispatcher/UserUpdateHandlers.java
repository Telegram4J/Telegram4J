package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.user.*;
import telegram4j.core.object.ChatPhoto;
import telegram4j.core.object.Id;
import telegram4j.mtproto.store.UserNameFields;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Instant;
import java.util.Optional;

import static telegram4j.core.util.EntityFactory.createUserStatus;
import static telegram4j.mtproto.util.TlEntityUtil.unmapEmpty;

class UserUpdateHandlers {
    // State handler
    // =====================

    static Mono<Void> handleStateUpdateChannelUserTyping(UpdateContext<UpdateChannelUserTyping> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChannelUserTyping(context.getUpdate());
    }

    static Mono<Void> handleStateUpdateChatUserTyping(UpdateContext<UpdateChatUserTyping> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatUserTyping(context.getUpdate());
    }

    static Mono<UserNameFields> handleStateUpdateUserName(UpdateContext<UpdateUserName> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserNameUpdate(context.getUpdate());
    }

    static Mono<UserProfilePhoto> handleStateUpdateUserPhoto(UpdateContext<UpdateUserPhoto> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserPhotoUpdate(context.getUpdate());
    }

    static Mono<String> handleStateUpdateUserPhone(UpdateContext<UpdateUserPhone> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserPhoneUpdate(context.getUpdate());
    }

    static Mono<UserStatus> handleStateUpdateUserStatus(UpdateContext<UpdateUserStatus> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserStatusUpdate(context.getUpdate());
    }

    static Mono<Void> handleStateUpdateUserTyping(UpdateContext<UpdateUserTyping> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserTyping(context.getUpdate());
    }

    // Update handler
    // =====================

    static Flux<UpdateChannelUserTypingEvent> handleUpdateChannelUserTyping(StatefulUpdateContext<UpdateChannelUserTyping, Void> context) {
        Id channelId = Id.ofChannel(context.getUpdate().channelId(), null);
        Id fromId = Id.of(context.getUpdate().fromId());

        return Flux.just(new UpdateChannelUserTypingEvent(context.getClient(), channelId, fromId,
                context.getUpdate().action(), context.getUpdate().topMsgId()));
    }

    static Flux<UpdateChatUserTypingEvent> handleUpdateChatUserTyping(StatefulUpdateContext<UpdateChatUserTyping, Void> context) {
        Id chatId = Id.ofChannel(context.getUpdate().chatId(), null);
        Id fromId = Id.of(context.getUpdate().fromId());

        return Flux.just(new UpdateChatUserTypingEvent(context.getClient(), chatId,
                fromId, context.getUpdate().action()));
    }

    static Flux<UpdateUserNameEvent> handleUpdateUserName(StatefulUpdateContext<UpdateUserName, UserNameFields> context) {
        UpdateUserName upd = context.getUpdate();

        Id userId = Id.ofUser(upd.userId(), null);
        UserNameFields old = context.getOld();

        return Flux.just(new UpdateUserNameEvent(context.getClient(), userId, upd.firstName(), upd.lastName(), upd.username(), old));
    }

    static Flux<UpdateUserPhoneEvent> handleUpdateUserPhone(StatefulUpdateContext<UpdateUserPhone, String> context) {
        Id userId = Id.ofUser(context.getUpdate().userId(), null);
        String old = context.getOld();

        return Flux.just(new UpdateUserPhoneEvent(context.getClient(), userId, context.getUpdate().phone(), old));
    }

    static Flux<UpdateUserPhotoEvent> handleUpdateUserPhoto(StatefulUpdateContext<UpdateUserPhoto, UserProfilePhoto> context) {
        MTProtoTelegramClient client = context.getClient();

        long userId = context.getUpdate().userId();
        // resolve input user for correct file ref id refreshing
        return client.getMtProtoResources().getStoreLayout()
                .resolveUser(userId)
                .map(u -> Tuples.of(Id.of(u, client.getSelfId()), TlEntityUtil.toInputPeer(u)))
                .map(TupleUtils.function((id, peer) -> {
                    Instant timestamp = Instant.ofEpochSecond(context.getUpdate().date());
                    var currentPhoto = Optional.ofNullable(unmapEmpty(context.getUpdate().photo(), ChatPhotoFields.class))
                            .map(d -> new ChatPhoto(client, d, peer, -1))
                            .orElse(null);
                    var oldPhoto = Optional.ofNullable(unmapEmpty(context.getOld(), ChatPhotoFields.class))
                            .map(d -> new ChatPhoto(client, d, peer, -1))
                            .orElse(null);

                    return new UpdateUserPhotoEvent(context.getClient(), id, timestamp,
                            currentPhoto, oldPhoto, context.getUpdate().previous());
                }))
                .flux();
    }

    static Flux<UpdateUserStatusEvent> handleUpdateUserStatus(StatefulUpdateContext<UpdateUserStatus, UserStatus> context) {
        Id userId = Id.ofUser(context.getUpdate().userId(), null);
        var currentStatus = createUserStatus(context.getClient(), context.getUpdate().status());
        var oldStatus = Optional.ofNullable(context.getOld())
                .map(d -> createUserStatus(context.getClient(), d))
                .orElse(null);

        return Flux.just(new UpdateUserStatusEvent(context.getClient(), userId, currentStatus, oldStatus));
    }

    static Flux<UpdateUserTypingEvent> handleUpdateUserTyping(StatefulUpdateContext<UpdateUserTyping, Void> context) {
        Id userId = Id.ofUser(context.getUpdate().userId(), null);

        return Flux.just(new UpdateUserTypingEvent(context.getClient(), userId, context.getUpdate().action()));
    }
}
