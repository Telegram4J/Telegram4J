package telegram4j.core.object.replymarkup;

import telegram4j.json.ReplyMarkupData;

import java.util.Optional;

public class ReplyKeyboardRemove extends ReplyMarkup {

    public ReplyKeyboardRemove(ReplyMarkupData data) {
        super(data);
    }

    public Optional<Boolean> isSelective() {
        return getData().selective();
    }

    @Override
    public String toString() {
        return "ReplyKeyboardRemove{} " + super.toString();
    }
}
