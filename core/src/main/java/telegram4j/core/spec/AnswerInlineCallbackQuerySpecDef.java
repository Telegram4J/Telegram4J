package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.spec.inline.InlineResultSpec;
import telegram4j.tl.InlineBotSwitchPM;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Value.Immutable
interface AnswerInlineCallbackQuerySpecDef extends Spec {

    @Value.Default
    default boolean gallery() {
        return false;
    }

    @Value.Default
    default boolean privacy() {
        return false;
    }

    List<InlineResultSpec> results();

    Duration cacheTime();

    Optional<String> nextOffset();

    Optional<InlineBotSwitchPM> switchPm();
}
