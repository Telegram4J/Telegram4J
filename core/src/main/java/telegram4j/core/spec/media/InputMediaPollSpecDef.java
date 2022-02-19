package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.EntityParser;
import telegram4j.core.util.EntityParserSupport;
import telegram4j.tl.InputMedia;
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
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        var parsed = parser()
                .flatMap(p -> solution().map(s -> EntityParserSupport.parse(client, p.apply(s))))
                .orElseGet(() -> Mono.just(Tuples.of(solution().orElse(""), List.of())));

        return parsed.map(TupleUtils.function((txt, ent) -> InputMediaPoll.builder()
                .poll(poll())
                .correctAnswers(correctAnswers().orElse(null))
                .solution(txt.isEmpty() ? null : txt)
                .solutionEntities(ent.isEmpty() ? null : ent)
                .build()));
    }
}
