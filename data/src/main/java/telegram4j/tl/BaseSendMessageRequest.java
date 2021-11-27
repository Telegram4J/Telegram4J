package telegram4j.tl;

import reactor.util.annotation.Nullable;

import java.util.List;

public interface BaseSendMessageRequest extends TlMethod<Updates> {

    @Nullable
    TlTrue noWebpage();

    @Nullable
    TlTrue silent();

    @Nullable
    TlTrue background();

    @Nullable
    TlTrue clearDraft();

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
