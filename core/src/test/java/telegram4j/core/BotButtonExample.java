package telegram4j.core;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import telegram4j.core.command.Command;
import telegram4j.core.command.TelegramCommand;
import telegram4j.core.event.domain.inline.CallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineCallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.spec.AnswerInlineCallbackQuerySpec;
import telegram4j.core.spec.BotCommandScopeSpec;
import telegram4j.core.spec.EditMessageSpec;
import telegram4j.core.spec.SendMessageSpec;
import telegram4j.core.spec.inline.InlineMessageSpec;
import telegram4j.core.spec.inline.InlineResultArticleSpec;
import telegram4j.core.spec.markup.InlineButtonSpec;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.mtproto.store.StoreLayoutImpl;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BotButtonExample {
    private static final List<Command> commands = List.of(new BeginInlineCommand());
    private static final Map<String, Command> commandsMap = commands.stream()
            .collect(Collectors.toUnmodifiableMap(c -> c.getInfo().command(), Function.identity()));

    public static void main(String[] args) {

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                .setDefaultEntityParserFactory(EntityParserFactory.MARKDOWN_V2)
                .setStoreLayout(new TestFileStoreLayout(new StoreLayoutImpl(Function.identity())))
                .withConnection(client -> {

                    Mono<Void> updateCommands = client.setCommands(BotCommandScopeSpec.of(BotCommandScopeSpec.Type.USERS),
                            "ru", commands.stream()
                                    .map(Command::getInfo)
                                    .collect(Collectors.toUnmodifiableList()))
                            .then();

                    Mono<Void> listenMessages = client.on(SendMessageEvent.class)
                            .flatMap(e -> {
                                String message = e.getMessage().getContent();
                                return Mono.justOrEmpty(e.getMessage().getEntities()
                                        .stream()
                                        .filter(p -> p.getType() == MessageEntity.Type.BOT_COMMAND &&
                                                p.getContent().equals(message))
                                        .map(MessageEntity::getContent)
                                        .findFirst())
                                        .map(s -> s.substring(1)) // substring first '/' char
                                        .mapNotNull(commandsMap::get)
                                        .flatMap(s -> Mono.from(s.execute(e)));
                            })
                            .then();

                    Mono<Void> listenCallbackQuery = client.on(CallbackQueryEvent.class)
                            .flatMap(e -> e.getChat().sendMessage("**Callback data:** " + e.getData()
                                    .map(ByteBufUtil::hexDump)
                                    .orElseThrow()))
                            .then();

                    Mono<Void> listenInlineQuery = client.on(InlineQueryEvent.class)
                            .flatMap(e -> e.answer(AnswerInlineCallbackQuerySpec.of(Duration.ZERO,
                                    List.of(InlineResultArticleSpec.of("The most updated and never abandoned site!",
                                            "https://core.telegram.org/schema", "one",
                                            InlineMessageSpec.text("Link to site: https://core.telegram.org/schema")
                                                    .withReplyMarkup(ReplyMarkupSpec.inlineKeyboard(List.of(
                                                            List.of(InlineButtonSpec.callback("Inline callback button",
                                                            false, Unpooled.copyLong(e.getQueryId())))))))))))
                            .then();

                    Mono<Void> listenInlineCallbackQuery = client.on(InlineCallbackQueryEvent.class)
                            .flatMap(e -> e.edit(EditMessageSpec.of()
                                    .withMessage("**Inline callback data:** " + e.getData()
                                            .map(ByteBufUtil::hexDump)
                                            .orElseThrow())))
                            .then();

                    return Mono.when(updateCommands, listenMessages,
                            listenCallbackQuery, listenInlineQuery, listenInlineCallbackQuery);
                })
                .block();
    }

    @TelegramCommand(command = "begin_inline", description = "Begin inline button demonstration")
    static class BeginInlineCommand implements Command {
        @Override
        public Publisher<?> execute(SendMessageEvent event) {
            return Mono.justOrEmpty(event.getChat())
                    .switchIfEmpty(event.getMessage().getChat())
                    .flatMap(chat -> chat.sendMessage(SendMessageSpec.of("Please select an inline button!")
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
}
