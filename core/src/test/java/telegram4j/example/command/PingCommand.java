package telegram4j.example.command;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.spec.EditMessageSpec;
import telegram4j.core.spec.SendMessageSpec;

@TelegramCommand(command = "ping", description = "Pong!")
public class PingCommand implements Command {
    @Override
    public Publisher<?> execute(SendMessageEvent event) {
        return Mono.justOrEmpty(event.getChat())
                .switchIfEmpty(event.getMessage().getChat())
                .flatMap(c -> {
                    long pre = System.currentTimeMillis();
                    return c.sendMessage(SendMessageSpec.of("Wait a second...")
                            .withReplyToMessageId(event.getMessage().getId()))
                            .flatMap(m -> m.edit(EditMessageSpec.builder()
                                    .message("Pong! " + (System.currentTimeMillis() - pre) + "ms.")
                                    .build()));
                });
    }
}
