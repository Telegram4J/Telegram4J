package telegram4j.core;

import telegram4j.core.event.MessageCreateEvent;
import telegram4j.core.spec.MessageCreateSpec;
import telegram4j.json.BotCommandData;
import telegram4j.json.BotCommandScopeData;
import telegram4j.json.BotCommandScopeType;
import telegram4j.json.MessageEntityType;
import telegram4j.json.request.SetMyCommandsRequest;

public class TelegramClientExample {

    public static void main(String[] args) {
        TelegramClient client = TelegramClient.create(System.getenv("T4J_TOKEN"));

        client.getRestClient().getCommandService()
                .setMyCommands(SetMyCommandsRequest.builder()
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
                        .map(list -> list.stream().anyMatch(entity ->
                                entity.getType() == MessageEntityType.BOT_COMMAND &&
                                entity.getContent().contains("shrug")))
                        .orElse(false))
                .flatMap(event -> client.sendMessage(MessageCreateSpec.builder()
                        .text("¯\\_(ツ)_/¯")
                        .chatId(event.getMessage().getChatId())
                        .build()))
                .subscribe();

        client.login().block();
    }
}
