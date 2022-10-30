package telegram4j.core.command;

import org.reactivestreams.Publisher;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.tl.BotCommand;
import telegram4j.tl.ImmutableBotCommand;

import java.util.Objects;

public interface Command {

    Publisher<?> execute(SendMessageEvent event);

    default BotCommand getInfo() {
        TelegramCommand info = getClass().getDeclaredAnnotation(TelegramCommand.class);
        Objects.requireNonNull(info, () -> "No @TelegramCommand annotation present on " + getClass().getCanonicalName());
        return ImmutableBotCommand.of(info.command(), info.description());
    }
}
