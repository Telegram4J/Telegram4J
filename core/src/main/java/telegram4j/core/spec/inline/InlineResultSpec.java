package telegram4j.core.spec.inline;

public interface InlineResultSpec {

    /** Unique id of result, must be non-empty and size must not exceed <b>64 bytes</b>. */
    String id();

    InlineMessageSpec message();
}
