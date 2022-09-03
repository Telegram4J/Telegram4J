package telegram4j.core.command;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.spec.SendMessageSpec;

@TelegramCommand(command = "echo", description = "Repeat text.")
public class EchoCommand implements Command {
    @Override
    public Publisher<?> execute(SendMessageEvent event) {
        return Mono.justOrEmpty(event.getChat())
                .zipWith(Mono.justOrEmpty(event.getMessage().getContent()))
                .flatMap(TupleUtils.function((c, t) -> {
                    int spc = t.indexOf(' ');
                    return c.sendMessage(SendMessageSpec.builder()
                            .message(spc == -1 ? "Missing echo text." : t.substring(spc + 1))
                            .replyToMessageId(event.getMessage().getId())
                            .build());
                }));
    }
}
