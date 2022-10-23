package telegram4j.core.spec;

import reactor.util.annotation.Nullable;
import telegram4j.core.spec.inline.InlineResultSpec;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.tl.ImmutableInlineBotSwitchPM;
import telegram4j.tl.InlineBotSwitchPM;
import telegram4j.tl.api.TlEncodingUtil;
import telegram4j.tl.request.messages.SetInlineBotResults;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class AnswerInlineCallbackQuerySpec {
    private final ImmutableEnumSet<Flag> flags;
    private final List<InlineResultSpec> results;
    private final Duration cacheTime;
    private final String nextOffset;
    private final InlineBotSwitchPM switchPm;

    private AnswerInlineCallbackQuerySpec(Duration cacheTime, Iterable<? extends InlineResultSpec> results) {
        this.cacheTime = Objects.requireNonNull(cacheTime);
        this.results = TlEncodingUtil.copyList(results);
        this.nextOffset = null;
        this.switchPm = null;
        this.flags = ImmutableEnumSet.of(Flag.class, 0);
    }

    private AnswerInlineCallbackQuerySpec(Builder builder) {
        this.results = List.copyOf(builder.results);
        this.cacheTime = builder.cacheTime;
        this.nextOffset = builder.nextOffset;
        this.switchPm = builder.switchPm;
        this.flags = ImmutableEnumSet.of(Flag.class, builder.flags);
    }

    private AnswerInlineCallbackQuerySpec(ImmutableEnumSet<Flag> flags, List<InlineResultSpec> results,
                                          Duration cacheTime, @Nullable String nextOffset,
                                          @Nullable InlineBotSwitchPM switchPm) {
        this.flags = flags;
        this.results = results;
        this.cacheTime = cacheTime;
        this.nextOffset = nextOffset;
        this.switchPm = switchPm;
    }

    public ImmutableEnumSet<Flag> flags() {
        return flags;
    }

    public List<InlineResultSpec> results() {
        return results;
    }

    public Duration cacheTime() {
        return cacheTime;
    }

    public Optional<String> nextOffset() {
        return Optional.ofNullable(nextOffset);
    }

    public Optional<InlineBotSwitchPM> switchPm() {
        return Optional.ofNullable(switchPm);
    }

    private AnswerInlineCallbackQuerySpec withFlags(Iterable<Flag> value) {
        Objects.requireNonNull(value);
        if (flags.equals(value)) return this;
        var newValue = ImmutableEnumSet.of(Flag.class, value);
        return new AnswerInlineCallbackQuerySpec(newValue, results, cacheTime, nextOffset, switchPm);
    }

    public AnswerInlineCallbackQuerySpec withResults(InlineResultSpec... elements) {
        var newValue = List.of(elements);
        if (results == newValue) return this;
        return new AnswerInlineCallbackQuerySpec(flags, newValue, cacheTime, nextOffset, switchPm);
    }

    public AnswerInlineCallbackQuerySpec withResults(Iterable<? extends InlineResultSpec> value) {
        if (results == value) return this;
        List<InlineResultSpec> newValue = TlEncodingUtil.copyList(value);
        if (results == newValue) return this;
        return new AnswerInlineCallbackQuerySpec(flags, newValue, cacheTime, nextOffset, switchPm);
    }

    public AnswerInlineCallbackQuerySpec withCacheTime(Duration value) {
        Objects.requireNonNull(value);
        if (cacheTime.equals(value)) return this;
        return new AnswerInlineCallbackQuerySpec(flags, results, value, nextOffset, switchPm);
    }

    public AnswerInlineCallbackQuerySpec withNextOffset(@Nullable String value) {
        if (Objects.equals(nextOffset, value)) return this;
        return new AnswerInlineCallbackQuerySpec(flags, results, cacheTime, value, switchPm);
    }

    public AnswerInlineCallbackQuerySpec withNextOffset(Optional<String> optional) {
        String value = optional.orElse(null);
        if (Objects.equals(nextOffset, value)) return this;
        return new AnswerInlineCallbackQuerySpec(flags, results, cacheTime, value, switchPm);
    }

    public AnswerInlineCallbackQuerySpec withSwitchPm(String text, String startParam) {
        return withSwitchPm(ImmutableInlineBotSwitchPM.of(text, startParam));
    }

    public AnswerInlineCallbackQuerySpec withSwitchPm(@Nullable InlineBotSwitchPM value) {
        if (switchPm == value) return this;
        return new AnswerInlineCallbackQuerySpec(flags, results, cacheTime, nextOffset, value);
    }

    public AnswerInlineCallbackQuerySpec withSwitchPm(Optional<? extends InlineBotSwitchPM> optional) {
        return withSwitchPm(optional.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnswerInlineCallbackQuerySpec)) return false;
        AnswerInlineCallbackQuerySpec that = (AnswerInlineCallbackQuerySpec) o;
        return flags.equals(that.flags)
                && results.equals(that.results)
                && cacheTime.equals(that.cacheTime)
                && Objects.equals(nextOffset, that.nextOffset)
                && Objects.equals(switchPm, that.switchPm);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + flags.hashCode();
        h += (h << 5) + results.hashCode();
        h += (h << 5) + cacheTime.hashCode();
        h += (h << 5) + Objects.hashCode(nextOffset);
        h += (h << 5) + Objects.hashCode(switchPm);
        return h;
    }

    @Override
    public String toString() {
        return "AnswerInlineCallbackQuerySpec{" +
                "flags=" + flags +
                ", results=" + results +
                ", cacheTime=" + cacheTime +
                ", nextOffset='" + nextOffset + '\'' +
                ", switchPm=" + switchPm +
                '}';
    }

    public static AnswerInlineCallbackQuerySpec of(Duration cacheTime, Iterable<? extends InlineResultSpec> results) {
        return new AnswerInlineCallbackQuerySpec(cacheTime, results);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_CACHE_TIME = 0x1;
        private static final byte INIT_BIT_RESULTS = 0x2;
        private byte initBits = 0x3;

        private Set<Flag> flags;
        private List<InlineResultSpec> results;
        private Duration cacheTime;
        private String nextOffset;
        private InlineBotSwitchPM switchPm;

        private Builder() {
        }

        public Builder from(AnswerInlineCallbackQuerySpec instance) {
            Objects.requireNonNull(instance);
            flags(instance.flags);
            addAllResults(instance.results);
            cacheTime(instance.cacheTime);
            nextOffset(instance.nextOffset);
            switchPm(instance.switchPm);
            return this;
        }

        public Builder flags(Set<Flag> flags) {
            this.flags = EnumSet.copyOf(flags);
            return this;
        }

        public Builder addResult(InlineResultSpec element) {
            Objects.requireNonNull(element);
            if (results == null) {
                results = new ArrayList<>();
                initBits &= ~INIT_BIT_RESULTS;
            }
            this.results.add(element);
            return this;
        }

        public Builder addResults(InlineResultSpec... elements) {
            this.results = Arrays.stream(elements)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            initBits &= ~INIT_BIT_RESULTS;
            return this;
        }

        public Builder results(Iterable<? extends InlineResultSpec> elements) {
            this.results = StreamSupport.stream(elements.spliterator(), false)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            initBits &= ~INIT_BIT_RESULTS;
            return this;
        }

        public Builder addAllResults(Iterable<? extends InlineResultSpec> elements) {
            List<InlineResultSpec> copy = StreamSupport.stream(elements.spliterator(), false)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            if (results == null) {
                results = copy;
                initBits &= ~INIT_BIT_RESULTS;
            } else {
                results.addAll(copy);
            }
            return this;
        }

        public Builder cacheTime(Duration cacheTime) {
            this.cacheTime = Objects.requireNonNull(cacheTime);
            initBits &= ~INIT_BIT_CACHE_TIME;
            return this;
        }

        public Builder nextOffset(@Nullable String nextOffset) {
            this.nextOffset = nextOffset;
            return this;
        }

        public Builder nextOffset(Optional<String> nextOffset) {
            this.nextOffset = nextOffset.orElse(null);
            return this;
        }

        public Builder switchPm(String text, String startParam) {
            this.switchPm = ImmutableInlineBotSwitchPM.of(text, startParam);
            return this;
        }

        public Builder switchPm(@Nullable InlineBotSwitchPM switchPm) {
            this.switchPm = switchPm;
            return this;
        }

        public Builder switchPm(Optional<? extends InlineBotSwitchPM> switchPm) {
            this.switchPm = switchPm.orElse(null);
            return this;
        }

        public AnswerInlineCallbackQuerySpec build() {
            if (initBits != 0) {
                throw new IllegalStateException(formatRequiredAttributesMessage());
            }
            return new AnswerInlineCallbackQuerySpec(this);
        }

        private String formatRequiredAttributesMessage() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_CACHE_TIME) != 0) attributes.add("cacheTime");
            if ((initBits & INIT_BIT_RESULTS) != 0) attributes.add("results");
            return "Cannot build AnswerInlineCallbackQuerySpec, some of required attributes are not set " + attributes;
        }
    }

    public enum Flag implements BitFlag {
        GALLERY(SetInlineBotResults.GALLERY_POS),
        PRIVATE(SetInlineBotResults.IS_PRIVATE_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }
    }
}
