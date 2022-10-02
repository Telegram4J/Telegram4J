package telegram4j.core.spec.inline;

import telegram4j.core.spec.Spec;

public interface InlineResultSpec extends Spec {

    /** Unique id of result, must be non-empty and size must not exceed <b>64 bytes</b>. */
    String id();

    InlineMessageSpec message();
}
