package telegram4j.tl;

import reactor.util.annotation.Nullable;

import java.util.List;

public interface BaseSendMessageRequest extends TlMethod<Updates> {

    default boolean noWebpage() {
        return false;
    }

    default boolean silent() {
        return false;
    }

    default boolean background() {
        return false;
    }

    default boolean clearDraft() {
        return false;
    }

    InputPeer peer();

    @Nullable
    Integer replyToMsgId();

    String message();

    long randomId();

    @Nullable
    ReplyMarkup replyMarkup();

    @Nullable
    List<MessageEntity> entities();

    @Nullable
    Integer scheduleDate();
}
