package telegram4j.core.spec.markup;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ReplyMarkupSpec {
    private final ReplyMarkup.Type type;
    @Nullable
    private final List<List<KeyboardButtonSpec>> rows;
    @Nullable
    private final Boolean singleUse;
    @Nullable
    private final Boolean selective;
    @Nullable
    private final Boolean resize;
    @Nullable
    private final String placeholder;

    private ReplyMarkupSpec(ReplyMarkup.Type type, @Nullable List<List<KeyboardButtonSpec>> rows,
                            @Nullable Boolean singleUse, @Nullable Boolean selective, @Nullable Boolean resize,
                            @Nullable String placeholder) {
        this.type = type;
        this.rows = rows;
        this.singleUse = singleUse;
        this.selective = selective;
        this.resize = resize;
        this.placeholder = placeholder;
    }

    public ReplyMarkup.Type type() {
        return type;
    }

    public Optional<List<List<KeyboardButtonSpec>>> rows() {
        return Optional.ofNullable(rows);
    }

    public Optional<Boolean> singleUse() {
        return Optional.ofNullable(singleUse);
    }

    public Optional<Boolean> selective() {
        return Optional.ofNullable(selective);
    }

    public Optional<Boolean> resize() {
        return Optional.ofNullable(resize);
    }

    public Optional<String> placeholder() {
        return Optional.ofNullable(placeholder);
    }

    public ReplyMarkupSpec withRows(@Nullable Iterable<? extends Iterable<? extends KeyboardButtonSpec>> values) {
        if (rows == values) return this;
        var newRows = values == null ? null : StreamSupport.stream(values.spliterator(), false)
                .map(TlEncodingUtil::<KeyboardButtonSpec>copyList)
                .collect(Collectors.toUnmodifiableList());
        if (rows == newRows) return this;
        return new ReplyMarkupSpec(type, newRows, singleUse, selective, resize, placeholder);
    }

    public ReplyMarkupSpec withPlaceholder(@Nullable String value) {
        if (Objects.equals(placeholder, value)) return this;
        return new ReplyMarkupSpec(type, rows, singleUse, selective, resize, value);
    }

    public ReplyMarkupSpec withPlaceholder(Optional<String> opt) {
        return withPlaceholder(opt.orElse(null));
    }

    public ReplyMarkupSpec withResize(@Nullable Boolean value) {
        if (Objects.equals(resize, value)) return this;
        return new ReplyMarkupSpec(type, rows, singleUse, selective, value, placeholder);
    }

    public ReplyMarkupSpec withResize(Optional<Boolean> opt) {
        return withResize(opt.orElse(null));
    }

    public ReplyMarkupSpec withSelective(@Nullable Boolean value) {
        if (Objects.equals(selective, value)) return this;
        return new ReplyMarkupSpec(type, rows, singleUse, value, resize, placeholder);
    }

    public ReplyMarkupSpec withSelective(Optional<Boolean> opt) {
        return withSelective(opt.orElse(null));
    }

    public ReplyMarkupSpec withSingleUse(@Nullable Boolean value) {
        if (Objects.equals(singleUse, value)) return this;
        return new ReplyMarkupSpec(type, rows, value, selective, resize, placeholder);
    }

    public ReplyMarkupSpec withSingleUse(Optional<Boolean> opt) {
        return withSingleUse(opt.orElse(null));
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplyMarkupSpec)) return false;
        ReplyMarkupSpec that = (ReplyMarkupSpec) o;
        return type.equals(that.type)
                && Objects.equals(rows, that.rows)
                && Objects.equals(singleUse, that.singleUse)
                && Objects.equals(selective, that.selective)
                && Objects.equals(resize, that.resize)
                && Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + Objects.hashCode(rows);
        h += (h << 5) + Objects.hashCode(singleUse);
        h += (h << 5) + Objects.hashCode(selective);
        h += (h << 5) + Objects.hashCode(resize);
        h += (h << 5) + Objects.hashCode(placeholder);
        return h;
    }

    @Override
    public String toString() {
        return "ReplyMarkupSpec{" +
                "type=" + type +
                ", rows=" + rows +
                ", singleUse=" + singleUse +
                ", selective=" + selective +
                ", resize=" + resize +
                ", placeholder='" + placeholder + '\'' +
                '}';
    }

    public static ReplyMarkupSpec inlineKeyboard(Iterable<? extends Iterable<InlineButtonSpec>> rows) {
        var rowsCopy = StreamSupport.stream(rows.spliterator(), false)
                .map(TlEncodingUtil::<KeyboardButtonSpec>copyList)
                .collect(Collectors.toUnmodifiableList());
        return new ReplyMarkupSpec(ReplyMarkup.Type.INLINE, rowsCopy, null, null, null, null);
    }

    public static ReplyMarkupSpec forceReplyKeyboard(boolean singleUse, boolean selective) {
        return new ReplyMarkupSpec(ReplyMarkup.Type.FORCE, null, singleUse, selective, null, null);
    }

    public static ReplyMarkupSpec hideKeyboard(boolean selective) {
        return new ReplyMarkupSpec(ReplyMarkup.Type.HIDE, null, null, selective, null, null);
    }

    public static ReplyMarkupSpec keyboard(boolean resize, boolean singleUse, boolean selective,
                                           @Nullable String placeholder, Iterable<? extends Iterable<ReplyButtonSpec>> rows) {
        var rowsCopy = StreamSupport.stream(rows.spliterator(), false)
                .map(TlEncodingUtil::<KeyboardButtonSpec>copyList)
                .collect(Collectors.toUnmodifiableList());
        return new ReplyMarkupSpec(ReplyMarkup.Type.KEYBOARD, rowsCopy,
                singleUse, selective, resize, placeholder);
    }
}
