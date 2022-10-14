package telegram4j.core.command;

import org.reactivestreams.Publisher;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.spec.EditMessageSpec;
import telegram4j.core.spec.SendMessageSpec;

@TelegramCommand(command = "ping", description = "Pong!")
public class PingCommand implements Command {
    @Override
    public Publisher<?> execute(SendMessageEvent event) {
        var chat = event.getChat().orElseThrow();
        long pre = System.currentTimeMillis();

        return chat.sendMessage(SendMessageSpec.of("Wait a second...")
                        .withReplyToMessageId(event.getMessage().getId()))
                .flatMap(m -> m.edit(EditMessageSpec.builder()
                        .message("Pong! " + (System.currentTimeMillis() - pre) + "ms.")
                        .build()));
    }
}
