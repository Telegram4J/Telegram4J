package telegram4j.core;

import io.netty.util.ResourceLeakDetector;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.command.Command;
import telegram4j.core.command.EchoCommand;
import telegram4j.core.command.PingCommand;
import telegram4j.core.command.ShrugCommand;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.MessageEntity;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.BotCommandScopeChats;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MTProtoBotExample {

    private static final Logger log = Loggers.getLogger(MTProtoBotExample.class);

    private static final List<Command> commands = List.of(new EchoCommand(), new ShrugCommand(), new PingCommand());
    private static final Map<String, Command> commandsMap = commands.stream()
            .collect(Collectors.toMap(c -> c.getInfo().command().toLowerCase(Locale.ROOT), Function.identity()));

    public static void main(String[] args) {

        // only for testing
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                .setStoreLayout(new TestFileStoreLayout(new StoreLayoutImpl(Function.identity())))
                // default params can't be generated for test sources
                .setInitConnectionParams(new InitConnectionParams("test", "test",
                        "en", "", "test", "en", null, null))
                .withConnection(client -> {

                    Mono<Void> updateCommands = client.getServiceHolder().getBotService()
                            .getBotCommands(BotCommandScopeChats.instance(), "en")
                            .collectList()
                            .flatMap(list -> {
                                var infos = commands.stream()
                                        .map(Command::getInfo)
                                        .collect(Collectors.toList());

                                if (list.equals(infos)) {
                                    return Mono.empty();
                                }
                                return client.getServiceHolder().getBotService()
                                        .setBotCommands(BotCommandScopeChats.instance(), "en", infos);
                            })
                            .then();

                    Mono<Void> listenMessages = client.on(SendMessageEvent.class)
                            .flatMap(e -> Mono.from(e.getMessage().getEntities()
                                    .filter(list -> !list.isEmpty() && list.get(0).getType() == MessageEntity.Type.BOT_COMMAND)
                                    .map(list -> list.get(0))
                                    .map(ent -> {
                                        String str = ent.getContent();
                                        int et = str.indexOf('@');
                                        String command = str.substring(str.indexOf('/') + 1, et != -1 ? et : str.length())
                                                .toLowerCase(Locale.ROOT);

                                        return Mono.fromSupplier(() -> commandsMap.get(command))
                                                .flatMap(c -> Mono.from(c.execute(e)))
                                                .then();
                                    })
                                    .orElseGet(Mono::empty)))
                            .then();

                    return Mono.when(updateCommands, listenMessages);
                })
                .block();
    }
}
