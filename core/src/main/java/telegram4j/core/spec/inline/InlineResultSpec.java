package telegram4j.core.spec.inline;

import telegram4j.core.spec.Spec;

public interface InlineResultSpec extends Spec {

    String id();

    InlineMessageSpec message();
}
