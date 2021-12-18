package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.user.*;
import telegram4j.mtproto.store.UserNameFields;
import telegram4j.tl.*;

public class UserUpdateHandlers {
    // State handler
    // =====================

    static Mono<Void> handleStateUpdateChannelUserTyping(UpdateContext<UpdateChannelUserTyping> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChannelUserTyping(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateChatUserTyping(UpdateContext<UpdateChatUserTyping> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onChatUserTyping(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<UserNameFields> handleStateUpdateUserName(UpdateContext<UpdateUserName> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserNameUpdate(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateUserPhoto(UpdateContext<UpdateUserPhoto> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserPhotoUpdate(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateUserPhone(UpdateContext<UpdateUserPhone> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserPhoneUpdate(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateUserStatus(UpdateContext<UpdateUserStatus> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserStatusUpdate(context.getUpdate(), context.getChats(), context.getUsers());
    }

    static Mono<Void> handleStateUpdateUserTyping(UpdateContext<UpdateUserTyping> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUserTyping(context.getUpdate(), context.getChats(), context.getUsers());
    }

    // Update handler
    // =====================

    static Flux<UpdateChannelUserTypingEvent> handleUpdateChannelUserTyping(StatefulUpdateContext<UpdateChannelUserTyping, Void> context) {
        return Flux.just(new UpdateChannelUserTypingEvent(context.getClient(), context.getUpdate().channelId(), context.getUpdate().fromId(), context.getUpdate().action(), context.getUpdate().topMsgId()) {
        });
    }

    static Flux<UpdateChatUserTypingEvent> handleUpdateChatUserTyping(StatefulUpdateContext<UpdateChatUserTyping, Void> context) {
        return Flux.just(new UpdateChatUserTypingEvent(context.getClient(), context.getUpdate().chatId(), context.getUpdate().fromId(), context.getUpdate().action()) {
        });
    }

    static Flux<UpdateUserNameEvent> handleUpdateUserName(StatefulUpdateContext<UpdateUserName, Void> context) {
        return Flux.just(new UpdateUserNameEvent(context.getClient(), context.getUpdate().userId(), context.getUpdate().firstName(), context.getUpdate().lastName(), context.getUpdate().username()) {
        });
    }

    static Flux<UpdateUserPhoneEvent> handleUpdateUserPhone(StatefulUpdateContext<UpdateUserPhone, Void> context) {
        return Flux.just(new UpdateUserPhoneEvent(context.getClient(), context.getUpdate().userId(), context.getUpdate().phone()) {
        });
    }

    static Flux<UpdateUserPhotoEvent> handleUpdateUserPhoto(StatefulUpdateContext<UpdateUserPhoto, Void> context) {
        return Flux.just(new UpdateUserPhotoEvent(context.getClient(), context.getUpdate().userId(), context.getUpdate().date(), context.getUpdate().photo(), context.getUpdate().previous()) {
        });
    }

    static Flux<UpdateUserStatusEvent> handleUpdateUserStatus(StatefulUpdateContext<UpdateUserStatus, Void> context) {
        return Flux.just(new UpdateUserStatusEvent(context.getClient(), context.getUpdate().userId(), context.getUpdate().status()) {
        });
    }

    static Flux<UpdateUserTypingEvent> handleUpdateUserTyping(StatefulUpdateContext<UpdateUserTyping, Void> context) {
        return Flux.just(new UpdateUserTypingEvent(context.getClient(), context.getUpdate().userId(), context.getUpdate().action()) {
        });
    }

}
