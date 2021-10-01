package telegram4j.core.event;

import telegram4j.core.TelegramClient;
import telegram4j.core.object.ChosenInlineResult;

public class ChosenInlineResultEvent extends Event {

    private final ChosenInlineResult chosenInlineResult;

    public ChosenInlineResultEvent(TelegramClient client, ChosenInlineResult chosenInlineResult) {
        super(client);
        this.chosenInlineResult = chosenInlineResult;
    }

    public ChosenInlineResult getChosenInlineResult() {
        return chosenInlineResult;
    }

    @Override
    public String toString() {
        return "ChosenInlineResultEvent{" +
                "chosenInlineResult=" + chosenInlineResult +
                "} " + super.toString();
    }
}
