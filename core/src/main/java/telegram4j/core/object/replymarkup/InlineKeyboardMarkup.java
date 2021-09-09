package telegram4j.core.object.replymarkup;

import telegram4j.json.ReplyMarkupData;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InlineKeyboardMarkup extends ReplyMarkup {

    public InlineKeyboardMarkup(ReplyMarkupData data) {
        super(data);
    }

    public static InlineKeyboardMarkup of(InlineKeyboardButton... buttons) {
        return new InlineKeyboardMarkup(ReplyMarkupData.builder()
                .inlineKeyboard(Arrays.stream(buttons)
                        .map(InlineKeyboardButton::getData)
                        .collect(Collectors.toList()))
                .build());
    }

    public static InlineKeyboardMarkup of(Iterable<InlineKeyboardButton> buttons) {
        return new InlineKeyboardMarkup(ReplyMarkupData.builder()
                .inlineKeyboard(StreamSupport.stream(buttons.spliterator(), false)
                        .map(InlineKeyboardButton::getData)
                        .collect(Collectors.toList()))
                .build());
    }

    public List<InlineKeyboardButton> getInlineKeyboard() {
        return getData().inlineKeyboard().stream()
                .map(InlineKeyboardButton::new)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "InlineKeyboardMarkup{} " + super.toString();
    }
}
