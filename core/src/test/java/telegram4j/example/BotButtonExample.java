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
package telegram4j.example;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ResourceLeakDetector;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.EventAdapter;
import telegram4j.core.event.domain.inline.CallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineCallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.spec.*;
import telegram4j.core.spec.inline.InlineMessageSpec;
import telegram4j.core.spec.inline.InlineResultArticleSpec;
import telegram4j.core.spec.markup.InlineButtonSpec;
import telegram4j.core.spec.markup.ReplyButtonSpec;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.example.command.Command;
import telegram4j.example.command.TelegramCommand;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BotButtonExample {
    private static final List<Command> commands = List.of(new BeginInlineCommand(), new BeginReplyCommand());
    private static final Map<String, Command> commandsMap = commands.stream()
            .collect(Collectors.toUnmodifiableMap(c -> c.getInfo().command(), Function.identity()));

    public static void main(String[] args) {
        // only for testing, do not copy it to your production code!!!
        Hooks.onOperatorDebug();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        var client = MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                .setDefaultEntityParserFactory(EntityParserFactory.MARKDOWN_V2)
                .setStoreLayout(new FileStoreLayout(new StoreLayoutImpl(Function.identity()),
                        Path.of("core/src/test/resources/t4j-bot.bin")))
                .connect()
                .block();

        Objects.requireNonNull(client);

        client.setCommands(BotCommandScopeSpec.of(BotCommandScopeSpec.Type.DEFAULT), "en", commands.stream()
                        .map(Command::getInfo)
                        .toList())
                .subscribe();

        client.on(new BotEventAdapter())
                .subscribe();

        client.onDisconnect().block();
    }

    @TelegramCommand(command = "begin_inline", description = "Begin inline button demonstration")
    static class BeginInlineCommand implements Command {
        @Override
        public Publisher<?> execute(SendMessageEvent event) {
            return Mono.justOrEmpty(event.getChat())
                    .switchIfEmpty(event.getMessage().getChat())
                    .flatMap(chat -> chat.sendMessage(SendMessageSpec.of("Please select an inline button!")
                            .withReplyTo(ReplyToMessageSpec.of(event.getMessage()))
                            .withReplyMarkup(ReplyMarkupSpec.inlineKeyboard(List.of(
                                    List.of(InlineButtonSpec.callback("Callback button", false,
                                                    Unpooled.copyInt(ThreadLocalRandom.current().nextInt())),
                                            InlineButtonSpec.userProfile("User profile redirect button",
                                                    event.getMessage().getAuthorId()
                                                            .orElseGet(event.getClient()::getSelfId))),
                                    List.of(InlineButtonSpec.url("Url button", "https://www.google.com/"),
                                            InlineButtonSpec.switchInline("Switch to inline query", true, "")))))));
        }
    }

    @TelegramCommand(command = "begin_reply", description = "Begin reply button demonstration")
    static class BeginReplyCommand implements Command {
        static final ReplyMarkupSpec groupsMarkup = ReplyMarkupSpec.keyboard(null, List.of(
                        List.of(ReplyButtonSpec.text("Just text button"),
                                ReplyButtonSpec.text("Another text button"))),
                EnumSet.allOf(ReplyMarkup.Flag.class));

        static final ReplyMarkupSpec dmMarkup = ReplyMarkupSpec.keyboard(null, List.of(
                        List.of(ReplyButtonSpec.requestPoll("Request an quiz poll", true),
                                ReplyButtonSpec.requestPoll("Request a regular poll", false),
                                ReplyButtonSpec.requestPoll("Request any poll", null))),
                EnumSet.allOf(ReplyMarkup.Flag.class));

        @Override
        public Publisher<?> execute(SendMessageEvent event) {
            return Mono.justOrEmpty(event.getChat())
                    .switchIfEmpty(event.getMessage().getChat())
                    .flatMap(chat -> chat.sendMessage(SendMessageSpec.of("Please select a reply button! " +
                                    "_Note: I have different keyboards in groups and private chats_")
                            .withReplyMarkup(chat.getType() == Chat.Type.PRIVATE ? dmMarkup : groupsMarkup)
                            .withReplyTo(ReplyToMessageSpec.of(event.getMessage()))));
        }
    }

    static class BotEventAdapter extends EventAdapter {
        @Override
        public Publisher<?> onInlineCallbackQuery(InlineCallbackQueryEvent event) {
            return event.edit(EditMessageSpec.of()
                    .withMessage("**Inline callback data:** " + event.getData()
                            .map(ByteBufUtil::hexDump)
                            .orElseThrow()));
        }

        @Override
        public Publisher<?> onInlineQuery(InlineQueryEvent event) {
            return event.answer(AnswerInlineCallbackQuerySpec.of(Duration.ZERO,
                    List.of(InlineResultArticleSpec.of("The most updated and never abandoned site!",
                            "https://core.telegram.org/schema", "one",
                            InlineMessageSpec.text("Link to site: https://core.telegram.org/schema")
                                    .withReplyMarkup(ReplyMarkupSpec.inlineKeyboard(List.of(
                                            List.of(InlineButtonSpec.callback("Inline callback button",
                                                    false, Unpooled.copyLong(event.getQueryId()))))))))));
        }

        @Override
        public Publisher<?> onCallbackQuery(CallbackQueryEvent event) {
            return event.getChat().sendMessage("**Callback data:** " + event.getData()
                    .map(ByteBufUtil::hexDump)
                    .orElseThrow());
        }

        @Override
        public Publisher<?> onSendMessage(SendMessageEvent event) {
            String message = event.getMessage().getContent();
            return Mono.justOrEmpty(event.getMessage().getEntities().stream()
                            .filter(p -> p.getType() == MessageEntity.Type.BOT_COMMAND &&
                                    p.getContent().equals(message)) // in this case will always allow commands without args
                            .map(MessageEntity::getContent)
                            .findFirst())
                    .map(s -> s.substring(1)) // substring first '/' char
                    .mapNotNull(commandsMap::get)
                    .flatMap(s -> Mono.from(s.execute(event)));
        }
    }
}
