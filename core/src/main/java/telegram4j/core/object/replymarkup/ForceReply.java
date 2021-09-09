package telegram4j.core.object.replymarkup;

import telegram4j.json.ReplyMarkupData;

import java.util.Optional;

public class ForceReply extends ReplyMarkup {

    public ForceReply(ReplyMarkupData data) {
        super(data);
    }

    public Optional<String> isInputFieldPlaceholder() {
        return getData().inputFieldPlaceholder();
    }

    public Optional<Boolean> isSelective() {
        return getData().selective();
    }
}
