package telegram4j.core.object.replymarkup;

import telegram4j.json.ReplyMarkupData;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ReplyKeyboardMarkup extends ReplyMarkup {

    public ReplyKeyboardMarkup(ReplyMarkupData data) {
        super(data);
    }

    public List<List<KeyboardButton>> getKeyboard() {
        return getData().keyboard()
                .orElseThrow(IllegalStateException::new).stream()
                .map(list -> list.stream()
                        .map(KeyboardButton::new)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    public Optional<Boolean> isResizeKeyboard() {
        return getData().resizeKeyboard();
    }

    public Optional<Boolean> isOneTimeKeyboard() {
        return getData().oneTimeKeyboard();
    }

    public Optional<String> isInputFieldPlaceholder() {
        return getData().inputFieldPlaceholder();
    }

    public Optional<Boolean> isSelective() {
        return getData().selective();
    }

    @Override
    public String toString() {
        return "ReplyKeyboardMarkup{} " + super.toString();
    }
}
