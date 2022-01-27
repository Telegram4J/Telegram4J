package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.core.util.EntityParser;
import telegram4j.core.util.EntityParserSupport;
import telegram4j.tl.InputMediaPoll;
import telegram4j.tl.Poll;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Value.Immutable(builder = false)
interface InputMediaPollSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.POLL;
    }

    Poll poll();

    Optional<List<byte[]>> correctAnswers();

    Optional<String> solution();

    Optional<Function<String, EntityParser>> parser();

    @Override
    default InputMediaPoll asData() {
        var formattedText = parser()
                .flatMap(p -> solution().map(s -> EntityParserSupport.parse(p.apply(s))))
                .orElse(null);

        return InputMediaPoll.builder()
                .poll(poll())
                .correctAnswers(correctAnswers().orElse(null))
                .solution(formattedText != null ? formattedText.getT1() : null)
                .solutionEntities(formattedText != null ? formattedText.getT2() : null)
                .build();
    }
}
