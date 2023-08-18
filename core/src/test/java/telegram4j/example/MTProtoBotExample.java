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

import io.netty.util.ResourceLeakDetector;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.PreferredEntityRetriever.Setting;
import telegram4j.core.spec.BotCommandScopeSpec;
import telegram4j.core.spec.BotCommandScopeSpec.Type;
import telegram4j.example.command.Command;
import telegram4j.example.command.EchoCommand;
import telegram4j.example.command.PingCommand;
import telegram4j.example.command.ShrugCommand;
import telegram4j.mtproto.MTProtoRetrySpec;
import telegram4j.mtproto.MethodPredicate;
import telegram4j.mtproto.ResponseTransformer;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;

import java.nio.file.Path;
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

        // only for testing, do not copy it to your production code!!!
        Hooks.onOperatorDebug();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);




        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                // prefer retrieving full data about peer entities
                .setEntityRetrieverStrategy(EntityRetrievalStrategy.preferred(
                        EntityRetrievalStrategy.STORE_FALLBACK_RPC, Setting.FULL, Setting.FULL))
                .setStoreLayout(new FileStoreLayout(new StoreLayoutImpl(Function.identity()),
                        Path.of("core/src/test/resources/t4j-bot.bin")))
                .addResponseTransformer(ResponseTransformer.retryFloodWait(MethodPredicate.all(),
                        MTProtoRetrySpec.max(d -> d.getSeconds() < 30, 2)))
                .withConnection(client -> {

                    Mono<Void> updateCommands = client.getCommands(BotCommandScopeSpec.of(Type.CHATS), "en")
                            .flatMap(list -> {
                                var infos = commands.stream()
                                        .map(Command::getInfo)
                                        .collect(Collectors.toUnmodifiableList());

                                if (list.equals(infos)) {
                                    return Mono.empty();
                                }
                                return client.setCommands(BotCommandScopeSpec.of(Type.CHATS), "en", infos);
                            })
                            .then();

                    // If your bot doesn't respond to any message in the group,
                    // then try to disable a privacy mode in the BotFather's settings:
                    // /setprivacy -> <choose your bot> -> Disable
                    Mono<Void> listenMessages = client.on(SendMessageEvent.class)
                            .flatMap(e -> Mono.just(e.getMessage().getEntities())
                                    .filter(list -> !list.isEmpty() && list.get(0).getType() == MessageEntity.Type.BOT_COMMAND)
                                    .map(list -> list.get(0))
                                    .flatMap(ent -> {
                                        String str = ent.getContent();
                                        int et = str.indexOf('@');
                                        String command = str.substring(str.indexOf('/') + 1, et != -1 ? et : str.length())
                                                .toLowerCase(Locale.ROOT);

                                        return Mono.fromSupplier(() -> commandsMap.get(command))
                                                .flatMap(c -> Mono.from(c.execute(e)))
                                                .then();
                                    }))
                            .then();

                    return Mono.when(updateCommands, listenMessages);
                })
                .block();
    }
}
