package telegram4j.core.command;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.spec.SendMessageSpec;

@TelegramCommand(command = "echo", description = "Repeat text.")
public class EchoCommand implements Command {
    @Override
    public Publisher<?> execute(SendMessageEvent event) {
        return Mono.justOrEmpty(event.getChat())
                .flatMap(c -> {
                    String text = event.getMessage().getContent();
                    int spc = text.indexOf(' ');
                    return c.sendMessage(SendMessageSpec.builder()
                            .message(spc == -1 ? "Missing echo text." : text.substring(spc + 1))
                            .replyToMessageId(event.getMessage().getId())
                            .build());
                });
    }
}
