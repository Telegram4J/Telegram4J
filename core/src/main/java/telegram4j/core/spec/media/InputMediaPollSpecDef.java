package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaPoll;
import telegram4j.tl.Poll;

import java.util.List;
import java.util.Optional;

@Value.Immutable
interface InputMediaPollSpecDef extends InputMediaSpec {

    Poll poll();

    Optional<List<byte[]>> correctAnswers();

    Optional<String> solution();

    Optional<EntityParserFactory> parser();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        var parsed = parser()
                .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                .flatMap(p -> solution().map(s -> EntityParserSupport.parse(client, p.apply(s.trim()))))
                .orElseGet(() -> Mono.just(Tuples.of(solution().orElse(""), List.of())));

        return parsed.map(TupleUtils.function((txt, ent) -> InputMediaPoll.builder()
                .poll(poll())
                .correctAnswers(correctAnswers().orElse(null))
                .solution(txt.isEmpty() ? null : txt)
                .solutionEntities(ent.isEmpty() ? null : ent)
                .build()));
    }
}
