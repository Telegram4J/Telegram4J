package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.core.event.MessageCreateEvent;
import telegram4j.json.BotCommandData;
import telegram4j.json.BotCommandScopeData;
import telegram4j.json.BotCommandScopeType;
import telegram4j.json.MessageEntityType;
import telegram4j.json.request.MessageCreate;
import telegram4j.json.request.SetMyCommands;
import telegram4j.rest.route.Routes;

public class TelegramClientExample {

    public static void main(String[] args) {
        TelegramClient client = TelegramClient.create(System.getenv("T4J_TOKEN"));

        client.getRestClient().getCommandService()
                .setMyCommands(SetMyCommands.builder()
                        .addCommand(BotCommandData.builder()
                                .command("shrug")
                                .description("¯\\_(ツ)_/¯")
                                .build())
                        .scope(BotCommandScopeData.builder()
                                .type(BotCommandScopeType.ALL_GROUP_CHATS)
                                .build())
                        .build())
                .block();

        client.on(MessageCreateEvent.class)
                .filter(event -> event.getMessage().getEntities()
                        .map(list -> list.stream().anyMatch(entity -> entity.getType() == MessageEntityType.BOT_COMMAND))
                        .map(bool -> bool && event.getMessage().getText()
                                .map(s -> s.contains("/shrug"))
                                .orElse(false))
                        .orElse(false))
                .flatMap(event -> client.getRestClient().getChatService()
                        .sendMessage(MessageCreate.builder()
                                .text("¯\\_(ツ)_/¯")
                                .chatId(event.getMessage().getChat().getId().asLong())
                                .build()))
                .subscribe();

        client.on(MessageCreateEvent.class)
                .filter(event -> event.getMessage().getText()
                        .map(s -> s.equals("амогус"))
                        .orElse(false))
                .flatMap(event -> {
                    long time = System.currentTimeMillis();
                    return client.getRestClient().getChatService()
                            .sendMessage(MessageCreate.builder()
                                    .chatId(event.getMessage().getChat().getId().asLong())
                                    .text("amogus")
                                    .build())
                            .then(Mono.fromRunnable(() -> System.out.println("Executed for " +
                                    (System.currentTimeMillis() - time) + "ms")));
                })
                .subscribe();

        client.login().block();
    }
}
