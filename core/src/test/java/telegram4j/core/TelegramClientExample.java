package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.core.event.MessageCreateEvent;
import telegram4j.json.request.MessageCreate;

public class TelegramClientExample {

    public static void main(String[] args) {
        TelegramClient telegramClient = TelegramClient.create(System.getenv("T4J_TOKEN"));

        telegramClient.on(MessageCreateEvent.class)
                .filter(event -> event.getMessage().getText()
                        .map(s -> s.equals("амогус"))
                        .orElse(false))
                .flatMap(event -> {
                    long time = System.currentTimeMillis();
                    return telegramClient.getRestClient().getChatService()
                            .sendMessage(MessageCreate.builder()
                                    .chatId(event.getMessage().getChat().getId().asLong())
                                    .text("amogus")
                                    .build())
                            .then(Mono.fromRunnable(() -> System.out.println("Executed for " +
                                    (System.currentTimeMillis() - time) + "ms")));
                })
                .subscribe();

        telegramClient.login().block();
    }
}
