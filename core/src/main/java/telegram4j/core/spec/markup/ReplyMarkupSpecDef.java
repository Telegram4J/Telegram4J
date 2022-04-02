package telegram4j.core.spec.markup;

import org.immutables.value.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.spec.Spec;
import telegram4j.tl.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
abstract class ReplyMarkupSpecDef implements Spec {

    public static ReplyMarkupSpec from(ReplyMarkup object) {
        var builder = ReplyMarkupSpec.builder()
                .type(object.getType());

        switch (object.getType()) {
            case KEYBOARD: {
                var c = (telegram4j.core.object.markup.ReplyKeyboardMarkup) object;

                builder.resize(c.isResize());
                builder.singleUse(c.isSingleUse());
                builder.selective(c.isSelective());
                builder.placeholder(c.getPlaceholder());
                builder.rows(c.getRows().stream()
                        .map(r -> r.stream()
                                .map(KeyboardButtonSpec::from)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList()));
                break;
            }
            case INLINE: {
                var c = (telegram4j.core.object.markup.ReplyInlineMarkup) object;

                builder.rows(c.getRows().stream()
                        .map(r -> r.stream()
                                .map(KeyboardButtonSpec::from)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList()));
                break;
            }
            case FORCE: {
                var c = (telegram4j.core.object.markup.ReplyKeyboardForceReply) object;

                builder.singleUse(c.isSingleUse());
                builder.selective(c.isSelective());
                builder.placeholder(c.getPlaceholder());

                break;
            }
            case HIDE: {
                var c = (telegram4j.core.object.markup.ReplyKeyboardHide) object;

                builder.selective(c.isSelective());
                break;
            }
        }

        return builder.build();
    }

    public static ReplyMarkupSpec inlineKeyboard(List<List<InlineButtonSpec>> rows) {
        return ReplyMarkupSpec.of(ReplyMarkup.Type.INLINE)
                .withRows(rows);
    }

    public static ReplyMarkupSpec forceReplyKeyboard(boolean singleUse, boolean selective) {
        return ReplyMarkupSpec.of(ReplyMarkup.Type.FORCE)
                .withSingleUse(singleUse)
                .withSelective(selective);
    }

    public static ReplyMarkupSpec hideKeyboard(boolean selective) {
        return ReplyMarkupSpec.of(ReplyMarkup.Type.HIDE)
                .withSelective(selective);
    }

    public static ReplyMarkupSpec keyboard(boolean resize, boolean singleUse, boolean selective,
                                           @Nullable String placeholder, List<List<ReplyButtonSpec>> rows) {
        return ReplyMarkupSpec.of(ReplyMarkup.Type.KEYBOARD)
                .withResize(resize)
                .withSingleUse(singleUse)
                .withSelective(selective)
                .withPlaceholder(Optional.ofNullable(placeholder))
                .withRows(rows);
    }

    public abstract ReplyMarkup.Type type();

    public abstract Optional<List<? extends List<? extends KeyboardButtonSpec>>> rows();

    public abstract Optional<Boolean> singleUse();

    public abstract Optional<Boolean> selective();

    public abstract Optional<Boolean> resize();

    public abstract Optional<String> placeholder();

    public Mono<telegram4j.tl.ReplyMarkup> asData(MTProtoTelegramClient client) {
        return Mono.defer(() -> {
            switch (type()) {
                case KEYBOARD:
                    return Flux.fromIterable(rows().orElseThrow())
                            .flatMap(list -> Flux.fromIterable(list)
                                    .flatMap(s -> s.asData(client))
                                    .collectList()
                                    .map(l -> KeyboardButtonRow.builder().buttons(l).build()))
                            .collectList()
                            .map(rows -> ReplyKeyboardMarkup.builder()
                                    .selective(selective().orElse(false))
                                    .singleUse(singleUse().orElse(false))
                                    .resize(resize().orElse(false))
                                    .placeholder(placeholder().orElse(null))
                                    .rows(rows)
                                    .build());
                case HIDE:
                    return Mono.just(ReplyKeyboardHide.builder().selective(selective().orElseThrow()).build());
                case FORCE:
                    return Mono.just(ReplyKeyboardForceReply.builder()
                            .selective(selective().orElseThrow())
                            .singleUse(singleUse().orElseThrow())
                            .placeholder(placeholder().orElse(null))
                            .build());
                case INLINE:
                    return Flux.fromIterable(rows().orElseThrow())
                            .flatMap(list -> Flux.fromIterable(list)
                                    .flatMap(s -> s.asData(client))
                                    .collectList()
                                    .map(l -> KeyboardButtonRow.builder().buttons(l).build()))
                            .collectList()
                            .map(rows -> ReplyInlineMarkup.builder().rows(rows).build());
                default:
                    return Mono.error(new IllegalStateException());
            }
        });
    }
}
