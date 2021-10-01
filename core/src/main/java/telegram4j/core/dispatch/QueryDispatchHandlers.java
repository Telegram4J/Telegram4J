package telegram4j.core.dispatch;

import reactor.core.publisher.Mono;
import telegram4j.core.TelegramClient;
import telegram4j.core.event.*;
import telegram4j.core.object.*;

class QueryDispatchHandlers {

    static class InlineQueryCreate implements DispatchHandler<InlineQueryCreateEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().inlineQuery().isPresent();
        }

        @Override
        public Mono<InlineQueryCreateEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();
            InlineQuery inlineQuery = update.getUpdateData().inlineQuery()
                    .map(inlineQueryData -> new InlineQuery(client, inlineQueryData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new InlineQueryCreateEvent(client, inlineQuery));
        }
    }

    static class ChosenInlineResultCreate implements DispatchHandler<ChosenInlineResultEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().chosenInlineResult().isPresent();
        }

        @Override
        public Mono<ChosenInlineResultEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();
            ChosenInlineResult chosenInlineResult = update.getUpdateData().chosenInlineResult()
                    .map(cInlineQueryData -> new ChosenInlineResult(client, cInlineQueryData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new ChosenInlineResultEvent(client, chosenInlineResult));
        }
    }

    static class CallbackQueryCreate implements DispatchHandler<CallbackQueryEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().callbackQuery().isPresent();
        }

        @Override
        public Mono<CallbackQueryEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();
            CallbackQuery callbackQuery = update.getUpdateData().callbackQuery()
                    .map(callbackQueryData -> new CallbackQuery(client, callbackQueryData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new CallbackQueryEvent(client, callbackQuery));
        }
    }

    static class ShippingQueryCreate implements DispatchHandler<ShippingQueryEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().shippingQuery().isPresent();
        }

        @Override
        public Mono<ShippingQueryEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();
            ShippingQuery shippingQuery = update.getUpdateData().shippingQuery()
                    .map(shippingQueryData -> new ShippingQuery(client, shippingQueryData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new ShippingQueryEvent(client, shippingQuery));
        }
    }

    static class PreCheckoutQueryCreate implements DispatchHandler<PreCheckoutQueryEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().shippingQuery().isPresent();
        }

        @Override
        public Mono<PreCheckoutQueryEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();
            PreCheckoutQuery preCheckoutQuery = update.getUpdateData().preCheckoutQuery()
                    .map(preCheckoutQueryData -> new PreCheckoutQuery(client, preCheckoutQueryData))
                    .orElseThrow(IllegalStateException::new);

            return Mono.just(new PreCheckoutQueryEvent(client, preCheckoutQuery));
        }
    }
}
