package telegram4j.core.dispatch;

import reactor.core.publisher.Mono;
import telegram4j.core.TelegramClient;
import telegram4j.core.event.*;
import telegram4j.core.object.Message;
import telegram4j.core.object.Poll;
import telegram4j.core.object.PollAnswer;
import telegram4j.json.MessageData;

class ChatDispatchHandlers {

    static class MessageCreate implements DispatchHandler<MessageCreateEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().message().isPresent();
        }

        @Override
        public Mono<MessageCreateEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();
            Message message = update.getUpdateData().message()
                    .map(messageData -> new Message(client, messageData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new MessageCreateEvent(client, message));
        }
    }

    static class MessageUpdate implements DispatchHandler<MessageUpdateEvent, MessageData> {

        @Override
        public boolean canHandle(UpdateContext<MessageData> update) {
            return update.getUpdateData().editedMessage().isPresent();
        }

        @Override
        public Mono<MessageUpdateEvent> handle(UpdateContext<MessageData> update) {
            TelegramClient client = update.getClient();
            Message currentMessage = update.getUpdateData().editedMessage()
                    .map(messageData -> new Message(client, messageData))
                    .orElseThrow(IllegalStateException::new);

            Message oldMessage = update.getOldData() != null ? new Message(client, update.getOldData()) : null;

            return Mono.just(new MessageUpdateEvent(client, currentMessage, oldMessage));
        }
    }

    static class ChannelPostCreate implements DispatchHandler<ChannelPostCreateEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().channelPost().isPresent();
        }

        @Override
        public Mono<ChannelPostCreateEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();

            Message channelPost = update.getUpdateData().channelPost()
                    .map(messageData -> new Message(client, messageData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new ChannelPostCreateEvent(client, channelPost));
        }
    }

    static class ChannelPostUpdate implements DispatchHandler<ChannelPostUpdateEvent, MessageData> {

        @Override
        public boolean canHandle(UpdateContext<MessageData> update) {
            return update.getUpdateData().editedChannelPost().isPresent();
        }

        @Override
        public Mono<ChannelPostUpdateEvent> handle(UpdateContext<MessageData> update) {
            TelegramClient client = update.getClient();

            Message currentMessage = update.getUpdateData().editedChannelPost()
                    .map(messageData -> new Message(client, messageData))
                    .orElseThrow(IllegalStateException::new);

            Message oldMessage = update.getOldData() != null ? new Message(client, update.getOldData()) : null;

            return Mono.just(new ChannelPostUpdateEvent(client, currentMessage, oldMessage));
        }
    }

    static class PollCreate implements DispatchHandler<PollCreateEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().poll().isPresent();
        }

        @Override
        public Mono<PollCreateEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();

            Poll poll = update.getUpdateData().poll()
                    .map(pollData -> new Poll(client, pollData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new PollCreateEvent(client, poll));
        }
    }

    static class PollAnswerCreate implements DispatchHandler<PollAnswerEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().pollAnswer().isPresent();
        }

        @Override
        public Mono<PollAnswerEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();

            PollAnswer pollAnswer = update.getUpdateData().pollAnswer()
                    .map(pollAnswerData -> new PollAnswer(client, pollAnswerData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new PollAnswerEvent(client, pollAnswer));
        }
    }

}
