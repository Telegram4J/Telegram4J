package telegram4j.core.spec;

import telegram4j.core.internal.MonoSpec;
import telegram4j.tl.InputReplyTo;

public sealed interface ReplySpec extends MonoSpec<InputReplyTo>
        permits ReplyToMessageSpec, ReplyToStorySpec {
}
