package telegram4j.core;

import io.netty.buffer.ByteBufAllocator;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.spec.SendMessageSpec;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.BotCommand;
import telegram4j.tl.BotCommandScopeChats;
import telegram4j.tl.ImmutableBotCommand;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class MTProtoBotExample {

    private static final Logger log = Loggers.getLogger(MTProtoBotExample.class);

    private static final Map<String, Function<SendMessageEvent, ? extends Publisher<?>>> commands = Map.of(
            "shrug", e -> Mono.justOrEmpty(e.getChat())
                    .flatMap(c -> c.sendMessage(SendMessageSpec.builder()
                            .message("¯\\_(ツ)_/¯")
                            .replyToMessageId(e.getMessage().getId())
                            .build())),
            "echo", e -> Mono.justOrEmpty(e.getChat())
                    .zipWith(Mono.justOrEmpty(e.getMessage().getMessage()))
                    .flatMap(TupleUtils.function((c, t) -> {
                        int spc = t.indexOf(' ');
                        if (spc == -1) {
                            return c.sendMessage(SendMessageSpec.builder()
                                    .message("Missing echo text.")
                                    .build());
                        }
                        return c.sendMessage(SendMessageSpec.builder()
                                .message(t.substring(spc + 1))
                                .build());
                    })));

    private static final List<BotCommand> commandsInfo = List.of(
            ImmutableBotCommand.of("shrug", "¯\\_(ツ)_/¯"),
            ImmutableBotCommand.of("echo", "Repeat text."));

    public static void main(String[] args) {

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                .setStoreLayout(new TestFileStoreLayout(ByteBufAllocator.DEFAULT, new StoreLayoutImpl()))
                .withConnection(client -> {
                    Mono<Void> updateCommands = client.getServiceHolder().getBotService()
                            .getBotCommands(BotCommandScopeChats.instance(), "en")
                            .collectList()
                            .flatMap(list -> {
                                if (list.equals(commandsInfo)) {
                                    return Mono.empty();
                                }
                                return client.getServiceHolder().getBotService()
                                        .setBotCommands(BotCommandScopeChats.instance(), "en", commandsInfo);
                            })
                            .then();

                    Mono<Void> listenMessages = client.on(SendMessageEvent.class)
                            .flatMap(e -> Mono.from(e.getMessage().getEntities()
                                    .filter(list -> !list.isEmpty() && list.get(0).getType() == MessageEntity.Type.BOT_COMMAND)
                                    .map(list -> list.get(0))
                                    .map(ent -> {
                                        String str = ent.getContent();
                                        int et = str.indexOf('@');
                                        String command = str.substring(str.indexOf('/') + 1, et != -1 ? et : str.length());
                                        return Mono.fromSupplier(() -> commands.get(command.toLowerCase(Locale.ROOT)))
                                                .flatMap(c -> Mono.from(c.apply(e)))
                                                .then();
                                    })
                                    .orElseGet(Mono::empty)))
                            .then();

                    return Mono.when(updateCommands, listenMessages);
                })
                .block();
    }
}
