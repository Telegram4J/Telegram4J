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
import reactor.util.annotation.Nullable;
import telegram4j.core.event.domain.message.*;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.Message;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.media.Poll;
import telegram4j.core.object.media.PollResults;
import telegram4j.core.util.Id;
import telegram4j.core.util.Variant2;
import telegram4j.mtproto.store.object.MessagePoll;
import telegram4j.mtproto.store.object.ResolvedDeletedMessages;
import telegram4j.tl.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static telegram4j.core.internal.MappingUtil.getAuthor;

class MessageUpdateHandlers {

    // State handler
    // =====================

    static Mono<Void> persistUpdateNewMessage(UpdateContext<UpdateNewMessage> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message());
    }

    static Mono<Void> persistUpdateNewChannelMessage(UpdateContext<UpdateNewChannelMessage> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onNewMessage(context.getUpdate().message());
    }

    static Mono<telegram4j.tl.Message> persistUpdateEditMessage(UpdateContext<UpdateEditMessage> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onEditMessage(context.getUpdate().message());
    }

    static Mono<telegram4j.tl.Message> persistUpdateEditChannelMessage(UpdateContext<UpdateEditChannelMessage> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onEditMessage(context.getUpdate().message());
    }

    static Mono<ResolvedDeletedMessages> persistUpdateDeleteMessages(UpdateContext<UpdateDeleteMessages> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onDeleteMessages(context.getUpdate());
    }

    static Mono<ResolvedDeletedMessages> persistUpdateDeleteScheduledMessages(UpdateContext<UpdateDeleteScheduledMessages> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onDeleteMessages(context.getUpdate());
    }

    static Mono<ResolvedDeletedMessages> persistUpdateDeleteChannelMessages(UpdateContext<UpdateDeleteChannelMessages> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onDeleteMessages(context.getUpdate());
    }

    static Mono<Void> persistUpdatePinnedMessages(UpdateContext<UpdatePinnedMessages> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUpdatePinnedMessages(context.getUpdate());
    }

    static Mono<Void> persistUpdatePinnedChannelMessages(UpdateContext<UpdatePinnedChannelMessages> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .onUpdatePinnedMessages(context.getUpdate());
    }

    static Mono<MessagePoll> persistUpdateMessagePoll(UpdateContext<UpdateMessagePoll> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .getPollById(context.getUpdate().pollId());
    }

    static Mono<MessagePoll> persistUpdateMessagePollVote(UpdateContext<UpdateMessagePollVote> context) {
        return context.getClient()
                .getMtProtoResources().getStoreLayout()
                .getPollById(context.getUpdate().pollId());
    }

    // Update handler
    // =====================

    static Flux<SendMessageEvent> handleUpdateNewMessage0(StatefulUpdateContext<?, ?> ctx,
                                                          telegram4j.tl.Message m) {
        Variant2<BaseMessage, MessageService> data;
        Id peerId;
        if (m instanceof BaseMessage b) {
            data = Variant2.ofT1(b);
            peerId = Id.of(b.peerId());
        } else if (m instanceof MessageService s) {
            data = Variant2.ofT2(s);
            peerId = Id.of(s.peerId());
        } else {
            // Why? I have no idea, but it occurs.
            // MessageEmpty objects are literally useless and harm an API
            return Flux.empty();
        }

        Chat chat = ctx.getChatEntity(peerId).orElse(null);
        MentionablePeer author = getAuthor(data, chat, ctx.getClient(), ctx.getChats(), ctx.getUsers())
                .orElse(null);
        Id resolvedChatId = chat != null ? chat.getId() : peerId;
        Message newMessage = new Message(ctx.getClient(), data, resolvedChatId);

        return Flux.just(new SendMessageEvent(ctx.getClient(), newMessage, chat, author));
    }

    static Flux<SendMessageEvent> handleUpdateNewMessage(StatefulUpdateContext<UpdateNewMessage, Void> context) {
        return handleUpdateNewMessage0(context, context.getUpdate().message());
    }

    static Flux<SendMessageEvent> handleUpdateNewChannelMessage(StatefulUpdateContext<UpdateNewChannelMessage, Void> context) {
        return handleUpdateNewMessage0(context, context.getUpdate().message());
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage0(StatefulUpdateContext<?, telegram4j.tl.Message> ctx,
                                                           telegram4j.tl.Message m) {
        // Typically reaction adding on bots accounts
        if (m.equals(ctx.getOld())) {
            return Flux.empty();
        }

        Variant2<BaseMessage, MessageService> data;
        Id peerId;
        if (m instanceof BaseMessage b) {
            data = Variant2.ofT1(b);
            peerId = Id.of(b.peerId());
        } else if (m instanceof MessageService s) {
            data = Variant2.ofT2(s);
            peerId = Id.of(s.peerId());
        } else {
            return Flux.error(new IllegalStateException("Received MessageEmpty in UpdateNewMessage"));
        }

        Chat chat = ctx.getChatEntity(peerId).orElse(null);
        MentionablePeer author = getAuthor(data, chat, ctx.getClient(), ctx.getChats(), ctx.getUsers())
                .orElse(null);
        Id resolvedChatId = chat != null ? chat.getId() : peerId;
        Message oldMessage = Optional.ofNullable(ctx.getOld())
                .map(d -> EntityFactory.createMessage(ctx.getClient(), d, resolvedChatId))
                .orElse(null);
        Message newMessage = new Message(ctx.getClient(), data, resolvedChatId);

        return Flux.just(new EditMessageEvent(ctx.getClient(), newMessage, oldMessage, chat, author));
    }

    static Flux<EditMessageEvent> handleUpdateEditChannelMessage(StatefulUpdateContext<UpdateEditChannelMessage, telegram4j.tl.Message> context) {
        return handleUpdateEditMessage0(context, context.getUpdate().message());
    }

    static Flux<EditMessageEvent> handleUpdateEditMessage(StatefulUpdateContext<UpdateEditMessage, telegram4j.tl.Message> context) {
        return handleUpdateEditMessage0(context, context.getUpdate().message());
    }

    static Flux<DeleteMessagesEvent> handleUpdateDeleteMessages0(StatefulUpdateContext<?, ResolvedDeletedMessages> context,
                                                                 @Nullable Id derivedChatId, List<Integer> messageIds,
                                                                 boolean scheduled) {
        Id chatId = Optional.ofNullable(context.getOld())
                .map(r -> Id.of(r.peer(), context.getClient().getSelfId()))
                .orElse(derivedChatId);

        var oldMessages = Optional.ofNullable(context.getOld())
                .map(r -> r.messages().stream()
                        .map(d -> EntityFactory.createMessage(context.getClient(), d,
                                Objects.requireNonNull(chatId))) // must be present
                        .toList())
                .orElse(null);

        return Flux.just(new DeleteMessagesEvent(context.getClient(), chatId,
                scheduled, oldMessages, messageIds));
    }

    static Flux<DeleteMessagesEvent> handleUpdateDeleteChannelMessages(StatefulUpdateContext<UpdateDeleteChannelMessages, ResolvedDeletedMessages> context) {
        return handleUpdateDeleteMessages0(context, Id.ofChannel(context.getUpdate().channelId()),
                context.getUpdate().messages(), false);
    }

    static Flux<DeleteMessagesEvent> handleUpdateDeleteScheduledMessages(StatefulUpdateContext<UpdateDeleteScheduledMessages, ResolvedDeletedMessages> context) {
        return handleUpdateDeleteMessages0(context, Id.of(context.getUpdate().peer()),
                context.getUpdate().messages(), true);
    }

    static Flux<DeleteMessagesEvent> handleUpdateDeleteMessages(StatefulUpdateContext<UpdateDeleteMessages, ResolvedDeletedMessages> context) {
        return handleUpdateDeleteMessages0(context, null, context.getUpdate().messages(), false);
    }

    static Flux<UpdatePinnedMessagesEvent> handleUpdatePinnedChannelMessages(StatefulUpdateContext<UpdatePinnedChannelMessages, Void> context) {
        var upd = context.getUpdate();
        Id chatId = Id.ofChannel(upd.channelId());
        return Flux.just(new UpdatePinnedMessagesEvent(context.getClient(), upd.pinned(), chatId, upd.messages()));
    }

    static Flux<UpdatePinnedMessagesEvent> handleUpdatePinnedMessages(StatefulUpdateContext<UpdatePinnedMessages, Void> context) {
        var upd = context.getUpdate();
        Id chatId = Id.of(upd.peer());
        return Flux.just(new UpdatePinnedMessagesEvent(context.getClient(), upd.pinned(), chatId, upd.messages()));
    }

    static Flux<MessagePollResultsEvent> handleUpdateMessagePoll(StatefulUpdateContext<UpdateMessagePoll, MessagePoll> context) {
        var upd = context.getUpdate();

        Poll poll = Optional.ofNullable(context.getOld())
                .map(d -> new Poll(context.getClient(), d))
                .or(() -> Optional.ofNullable(context.getUpdate().poll())
                        .map(d -> new Poll(context.getClient(), d, null)))
                .orElse(null);
        PollResults results = new PollResults(context.getClient(), upd.results());

        return Flux.just(new MessagePollResultsEvent(context.getClient(), poll, results));
    }

    static Flux<MessagePollVoteEvent> handleUpdateMessagePollVote(StatefulUpdateContext<UpdateMessagePollVote, MessagePoll> context) {
        var upd = context.getUpdate();

        Poll poll = Optional.ofNullable(context.getOld())
                .map(d -> new Poll(context.getClient(), d))
                .orElse(null);
        var peer = context.getPeer(Id.of(upd.peer()))
                .orElseThrow();

        return Flux.just(new MessagePollVoteEvent(context.getClient(), poll, peer, upd.options()));
    }
}
