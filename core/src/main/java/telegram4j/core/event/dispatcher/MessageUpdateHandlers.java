package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.message.*;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.Message;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.media.Poll;
import telegram4j.core.object.media.PollResults;
import telegram4j.core.util.Id;
import telegram4j.mtproto.store.ResolvedDeletedMessages;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static telegram4j.core.internal.MappingUtil.getAuthor;

class MessageUpdateHandlers {

    // State handler
    // =====================

    static Mono<Void> handleStateUpdateNewMessage(UpdateContext<UpdateNewMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message());
    }

    static Mono<telegram4j.tl.Message> handleStateUpdateEditMessage(UpdateContext<UpdateEditMessageFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onEditMessage(context.getUpdate().message());
    }

    static Mono<ResolvedDeletedMessages> handleStateUpdateDeleteMessages(UpdateContext<UpdateDeleteMessagesFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onDeleteMessages(context.getUpdate());
    }

    static Mono<Void> handleStateUpdatePinnedMessages(UpdateContext<UpdatePinnedMessagesFields> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUpdatePinnedMessages(context.getUpdate());
    }

    // Update handler
    // =====================

    static Flux<SendMessageEvent> handleUpdateNewMessage(StatefulUpdateContext<UpdateNewMessageFields, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        BaseMessageFields message = (BaseMessageFields) context.getUpdate().message();

        Chat chat = context.getChatEntity(Id.of(message.peerId())).orElse(null);
        MentionablePeer author = getAuthor(context, message, chat)
                .orElse(null);
        Id resolvedChatId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(message.peerId()));

        Message newMessage = EntityFactory.createMessage(client, message, resolvedChatId);

        return Flux.just(new SendMessageEvent(client, newMessage, chat, author));
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage(StatefulUpdateContext<UpdateEditMessageFields, telegram4j.tl.Message> context) {
        MTProtoTelegramClient client = context.getClient();
        BaseMessageFields message = (BaseMessageFields) context.getUpdate().message();

        // Typically reaction adding on bots accounts
        if (message.equals(context.getOld())) {
            return Flux.empty();
        }

        Chat chat = context.getChatEntity(Id.of(message.peerId())).orElse(null);
        MentionablePeer author = getAuthor(context, message, chat)
                .orElse(null);
        Id resolvedId = Optional.ofNullable(chat)
                .map(Chat::getId)
                .orElseGet(() -> Id.of(message.peerId()));
        Message oldMessage = Optional.ofNullable(context.getOld())
                .map(d -> EntityFactory.createMessage(client, d, resolvedId))
                .orElse(null);
        Message newMessage = EntityFactory.createMessage(client, message, resolvedId);

        return Flux.just(new EditMessageEvent(client, newMessage, oldMessage, chat, author));
    }

    static Flux<DeleteMessagesEvent> handleUpdateDeleteMessages(StatefulUpdateContext<UpdateDeleteMessagesFields, ResolvedDeletedMessages> context) {
        Id chatId = Optional.ofNullable(context.getOld())
                .map(r -> Id.of(r.getPeer(), context.getClient().getSelfId()))
                .orElse(null);

        var oldMessages = Optional.ofNullable(context.getOld())
                .map(ResolvedDeletedMessages::getMessages)
                .map(l -> l.stream()
                        .map(d -> EntityFactory.createMessage(context.getClient(), d,
                                Objects.requireNonNull(chatId))) // must be present
                        .collect(Collectors.toUnmodifiableList()))
                .orElse(null);

        boolean scheduled = context.getUpdate().identifier() == UpdateDeleteScheduledMessages.ID;

        return Flux.just(new DeleteMessagesEvent(context.getClient(), chatId,
                scheduled, oldMessages, context.getUpdate().messages()));
    }

    static Flux<UpdatePinnedMessagesEvent> handleUpdatePinnedMessages(StatefulUpdateContext<UpdatePinnedMessagesFields, Void> context) {
        UpdatePinnedMessagesFields upd = context.getUpdate();

        Id chatId = upd.identifier() == UpdatePinnedMessages.ID
                ? Id.of(((UpdatePinnedMessages) upd).peer())
                : Id.ofChannel(((UpdatePinnedChannelMessages) upd).channelId(), null);

        return Flux.just(new UpdatePinnedMessagesEvent(context.getClient(), upd.pinned(), chatId, upd.messages()));
    }

    static Flux<MessagePollResultsEvent> handleUpdateMessagePoll(StatefulUpdateContext<UpdateMessagePoll, Void> context) {
        var upd = context.getUpdate();

        Poll poll = Optional.ofNullable(upd.poll())
                .map(Poll::new)
                .orElse(null);
        PollResults results = new PollResults(context.getClient(), upd.results(),
                -1, InputPeerEmpty.instance()); // TODO: support this type of context

        return Flux.just(new MessagePollResultsEvent(context.getClient(), upd.pollId(), poll, results));
    }

    static Flux<MessagePollVoteEvent> handleUpdateMessagePollVote(StatefulUpdateContext<UpdateMessagePollVote, Void> context) {
        var upd = context.getUpdate();

        var user = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.userId(), null)));

        return Flux.just(new MessagePollVoteEvent(context.getClient(), upd.pollId(), user, upd.options()));
    }
}
