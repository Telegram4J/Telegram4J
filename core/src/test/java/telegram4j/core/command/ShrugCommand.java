package telegram4j.core.command;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.spec.SendMessageSpec;

@TelegramCommand(command = "shrug", description = "¯\\_(ツ)_/¯")
public class ShrugCommand implements Command {
    @Override
    public Publisher<?> execute(SendMessageEvent event) {
        return Mono.justOrEmpty(event.getChat())
                .flatMap(c -> c.sendMessage(SendMessageSpec.builder()
                        .message("¯\\_(ツ)_/¯")
                        .replyToMessageId(event.getMessage().getId())
                        .build()));
    }
}
