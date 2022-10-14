package telegram4j.core.spec.markup;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.Preconditions;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ReplyMarkupSpec {
    private final ReplyMarkup.Type type;
    private final ImmutableEnumSet<ReplyMarkup.Flag> flags;
    @Nullable
    private final List<List<KeyboardButtonSpec>> rows;
    @Nullable
    private final String placeholder;

    private ReplyMarkupSpec(ReplyMarkup.Type type, @Nullable List<List<KeyboardButtonSpec>> rows,
                            ImmutableEnumSet<ReplyMarkup.Flag> flags, @Nullable String placeholder) {
        this.type = type;
        this.rows = rows;
        this.flags = flags;
        this.placeholder = placeholder;
    }

    public ReplyMarkup.Type type() {
        return type;
    }

    public Optional<List<List<KeyboardButtonSpec>>> rows() {
        return Optional.ofNullable(rows);
    }

    public ImmutableEnumSet<ReplyMarkup.Flag> flags() {
        return flags;
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
        return new ReplyMarkupSpec(type, newRows, flags, placeholder);
    }

    public ReplyMarkupSpec withPlaceholder(@Nullable String value) {
        if (Objects.equals(placeholder, value)) return this;
        return new ReplyMarkupSpec(type, rows, flags, value);
    }

    public ReplyMarkupSpec withPlaceholder(Optional<String> opt) {
        return withPlaceholder(opt.orElse(null));
    }

    public ReplyMarkupSpec withFlags(ReplyMarkup.Flag... values) {
        Objects.requireNonNull(values);
        var flagsCopy = ImmutableEnumSet.of(values);
        if (flags.getValue() == flagsCopy.getValue()) return this;
        return new ReplyMarkupSpec(type, rows, flagsCopy, placeholder);
    }

    public ReplyMarkupSpec withFlags(Iterable<ReplyMarkup.Flag> values) {
        Objects.requireNonNull(values);
        if (flags == values) return this;
        var flagsCopy = ImmutableEnumSet.of(ReplyMarkup.Flag.class, values);
        if (flags.getValue() == flagsCopy.getValue()) return this;
        return new ReplyMarkupSpec(type, rows, flagsCopy, placeholder);
    }

    public Mono<telegram4j.tl.ReplyMarkup> asData(MTProtoTelegramClient client) {
        return Mono.defer(() -> {
            switch (type()) {
                case KEYBOARD:
                    return Flux.fromIterable(rows().orElseThrow())
                            .flatMap(list -> Flux.fromIterable(list)
                                    .flatMap(s -> s.asData(client))
                                    .collectList()
                                    .map(ImmutableKeyboardButtonRow::of))
                            .collectList()
                            .map(rows -> ReplyKeyboardMarkup.builder()
                                    .flags(flags.getValue())
                                    .placeholder(placeholder)
                                    .rows(rows)
                                    .build());
                case HIDE:
                    return Mono.just(ImmutableReplyKeyboardHide.of(flags.getValue()));
                case FORCE_REPLY:
                    return Mono.just(ReplyKeyboardForceReply.builder()
                            .flags(flags.getValue())
                            .placeholder(placeholder)
                            .build());
                case INLINE:
                    return Flux.fromIterable(rows().orElseThrow())
                            .flatMap(list -> Flux.fromIterable(list)
                                    .flatMap(s -> s.asData(client))
                                    .collectList()
                                    .map(ImmutableKeyboardButtonRow::of))
                            .collectList()
                            .map(ImmutableReplyInlineMarkup::of);
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
                && flags.equals(that.flags)
                && Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + Objects.hashCode(rows);
        h += (h << 5) + flags.hashCode();
        h += (h << 5) + Objects.hashCode(placeholder);
        return h;
    }

    @Override
    public String toString() {
        return "ReplyMarkupSpec{" +
                "type=" + type +
                ", flags=" + flags +
                ", rows=" + rows +
                ", placeholder='" + placeholder + '\'' +
                '}';
    }

    public static ReplyMarkupSpec inlineKeyboard(Iterable<? extends Iterable<InlineButtonSpec>> rows) {
        var rowsCopy = StreamSupport.stream(rows.spliterator(), false)
                .map(TlEncodingUtil::<KeyboardButtonSpec>copyList)
                .collect(Collectors.toUnmodifiableList());
        return new ReplyMarkupSpec(ReplyMarkup.Type.INLINE, rowsCopy, ImmutableEnumSet.of(ReplyMarkup.Flag.class, 0), null);
    }

    public static ReplyMarkupSpec forceReplyKeyboard(Iterable<ReplyMarkup.Flag> flags) {
        var flagsCopy = ImmutableEnumSet.of(ReplyMarkup.Flag.class, flags);
        Preconditions.requireArgument(!flagsCopy.contains(ReplyMarkup.Flag.RESIZE), "Reply keyboards can't have resize option");
        return new ReplyMarkupSpec(ReplyMarkup.Type.FORCE_REPLY, null, flagsCopy, null);
    }

    public static ReplyMarkupSpec forceReplyKeyboard(ReplyMarkup.Flag... flags) {
        var flagsCopy = ImmutableEnumSet.of(flags);
        Preconditions.requireArgument(!flagsCopy.contains(ReplyMarkup.Flag.RESIZE), "Reply keyboards can't have resize option");
        return new ReplyMarkupSpec(ReplyMarkup.Type.FORCE_REPLY, null, flagsCopy, null);
    }

    public static ReplyMarkupSpec hideKeyboard() {
        return hideKeyboard(false);
    }

    public static ReplyMarkupSpec hideKeyboard(boolean selective) {
        var flags = ImmutableEnumSet.of(ReplyMarkup.Flag.class,
                selective ? ReplyKeyboardMarkup.SELECTIVE_MASK : 0);
        return new ReplyMarkupSpec(ReplyMarkup.Type.HIDE, null, flags, null);
    }

    public static ReplyMarkupSpec keyboard(Iterable<? extends Iterable<ReplyButtonSpec>> rows) {
        return keyboard(null, rows, Set.of());
    }

    public static ReplyMarkupSpec keyboard(@Nullable String placeholder, Iterable<? extends Iterable<ReplyButtonSpec>> rows,
                                           Iterable<ReplyMarkup.Flag> flags) {
        var flagsCopy = ImmutableEnumSet.of(ReplyMarkup.Flag.class, flags);
        var rowsCopy = StreamSupport.stream(rows.spliterator(), false)
                .map(TlEncodingUtil::<KeyboardButtonSpec>copyList)
                .collect(Collectors.toUnmodifiableList());
        return new ReplyMarkupSpec(ReplyMarkup.Type.KEYBOARD, rowsCopy, flagsCopy, placeholder);
    }

    public static ReplyMarkupSpec keyboard(@Nullable String placeholder, Iterable<? extends Iterable<ReplyButtonSpec>> rows,
                                           ReplyMarkup.Flag... flags) {
        var flagsCopy = ImmutableEnumSet.of(flags);
        var rowsCopy = StreamSupport.stream(rows.spliterator(), false)
                .map(TlEncodingUtil::<KeyboardButtonSpec>copyList)
                .collect(Collectors.toUnmodifiableList());
        return new ReplyMarkupSpec(ReplyMarkup.Type.KEYBOARD, rowsCopy, flagsCopy, placeholder);
    }
}
